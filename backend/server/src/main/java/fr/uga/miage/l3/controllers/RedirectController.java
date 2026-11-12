package fr.uga.miage.l3.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renvoie les routes de l'application Angular vers index.html : sans cela,
 * recharger la page sur /journees/2026-11-12 donnerait une erreur 404.
 */
@Controller
public class RedirectController {

    @GetMapping({
        "/",
        "/journees/**",
        "/journee/**",
        "/solutions/**",
        "/livraisons",
        "/equipe",
    })
    public String forwardToApplication() {
        return "forward:/index.html";
    }
}
