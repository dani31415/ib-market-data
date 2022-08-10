package dev.damaso.market.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.external.ibgw.Api;

@RestController
public class Login {
    @Autowired
    Api api;

    @GetMapping("/ib/login")
	public boolean snapshot() {
        api.reauthenticateHelper();
        return true;
    }
}
