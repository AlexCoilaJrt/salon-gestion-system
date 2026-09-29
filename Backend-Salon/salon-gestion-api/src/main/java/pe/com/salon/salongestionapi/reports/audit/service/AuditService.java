package pe.com.salon.salongestionapi.reports.audit.service;

import org.springframework.stereotype.Service;

@Service
public class AuditService {
    public void registerAction(String action, String module, String user, String details) {
        // Implementación pendiente para el sistema de Salón
        System.out.println("Auditoría: " + action + " en módulo " + module + " por " + user);
    }
    
    public void registerAction(String action, String module, String user) {
        registerAction(action, module, user, "");
    }
}
