package com.example.networkeditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.sld.SingleLineDiagram;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;


@RestController
@RequestMapping("/network")
@CrossOrigin(origins = "*")
public class Controller {

    private final NetworkState networkState;
    private final ObjectMapper objectMapper;
    private final Service service;

    Controller(NetworkState networkState) {
        this.networkState = networkState;
        this.service = new Service();
        this.objectMapper = new ObjectMapper();
    }


    // LOADER
    @PostMapping("/load")
    public Dto.loadNetworkResponse loadNetwork(@RequestBody Dto.loadNetworkRequest request) {
        Network network;

        try {
            network = Network.read(Path.of(request.file_path()));
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to read " + request.file_path() + " : " + e.getMessage(), e);
        }

        networkState.set(network);

        return new Dto.loadNetworkResponse(request.file_path(), new Dto.NetworkInfo(
                network.getId(),
                network.getSubstationCount(),
                network.getVoltageLevelCount()
        ));
    }

    // INFOS
    @GetMapping("/infos/voltage_levels")
    public List<String> getVoltageLevelsIds() {
        return networkState.get().getVoltageLevelStream().map(vl -> vl.getId()).toList();
    }

    @PostMapping("/sld")
    public Dto.SldResponse getSld(@RequestBody Dto.SldRequest request) {
        StringWriter svg = new StringWriter();
        StringWriter metadata = new StringWriter();

        try {
            SingleLineDiagram.draw(networkState.get(), request.vlId(), svg, metadata);
        }catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to get svg : " + e.getMessage(), e );
        }

        try {
            return new Dto.SldResponse(
                    svg.toString(),
                    objectMapper.readTree(metadata.toString()),
                    service.readProperties(networkState.get(), request.vlId()));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to get metadata : " + e.getMessage(), e );
        }
    }



    // MODIFICATORS

    @DeleteMapping("/elements/{elementId}")
    public Dto.ElementResponse deleteElement(@PathVariable String elementId){
        try {
            service.removeElement(networkState.get(), elementId);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to delete element : " + e.getMessage(), e);
        }
        return new Dto.ElementResponse("Element deleted", elementId);
    }

    @DeleteMapping("/bay/{elementId}")
    public Dto.ElementResponse deleteBay(@PathVariable String elementId){
        try {
            service.removeBay(networkState.get(), elementId);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to delete bay : " + e.getMessage(), e);
        }
        return new Dto.ElementResponse("bay deleted", elementId);
    }

    @PostMapping("/changes")
    public Dto.ApplyChangesResponse changeElement(@RequestBody Dto.ChangeEntry change){
        try {
            service.apply(networkState.get(), change);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to change element : " + e.getMessage(), e);
        }
        return new Dto.ApplyChangesResponse("Element changed", true);
    }






}
