package com.example.networkeditor;

import com.powsybl.iidm.network.Network;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class NetworkState {

    private Network network;

    public Network get() {
        if (network == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Network not found");
        }
        return network;
    }

    public void set(Network network) {
        this.network = network;
    }

}
