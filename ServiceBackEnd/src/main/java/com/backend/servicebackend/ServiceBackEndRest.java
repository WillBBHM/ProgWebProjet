package com.backend.servicebackend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
public class ServiceBackEndRest {

    private boolean isValidPortRange(String ports) {
        if (ports == null || ports.isEmpty()) return false;
        String[] portRanges = ports.split(",");
        for (String range : portRanges) {
            if (range.contains("-")) {
                String[] bounds = range.split("-");
                if (bounds.length != 2 || !isNumeric(bounds[0]) || !isNumeric(bounds[1])) {
                    return false;
                }
            } else if (!isNumeric(range)) {
                return false;
            }
        }
        return true;
    }

    private boolean isNumeric(String str) {
        return str.matches("\\d+");
    }

    @GetMapping("/")
    public String sayHello(){
        return "Hello";
    }

    @GetMapping("/money")
    public String Bloblo(){
        return "I want a lot of money";
    }

    @GetMapping(value = "/scan", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> scan(@RequestParam("target") String target,
                                       @RequestParam(value = "ports", defaultValue = "1-1024") String ports) {
        if (target == null || target.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("<error>Paramètre 'target' requis.</error>");
        }
        if (!isValidPortRange(ports)) {
            return ResponseEntity.badRequest().body("<error>Paramètre 'ports' invalide.</error>");
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "run", "--rm", "instrumentisto/nmap", "-p", ports, target, "-sV", "-oX", "-");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            long startTime = System.currentTimeMillis();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (System.currentTimeMillis() - startTime > 300000) { // Timeout après 5 minutes
                        process.destroy();
                        return ResponseEntity.status(500).body("<error>Le scan a dépassé le délai imparti.</error>");
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return ResponseEntity.status(500).body("<error>Le scan a échoué.</error>");
            }

            return ResponseEntity.ok().body(output.toString());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("<error>Le thread a été interrompu : " + e.getMessage() + "</error>");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("<error>Erreur lors de la lecture de la sortie du processus : " + e.getMessage() + "</error>");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<error>Erreur inattendue : " + e.getMessage() + "</error>");
        }
    }

}
