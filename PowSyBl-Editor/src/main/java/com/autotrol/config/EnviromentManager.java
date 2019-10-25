/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author cgalli
 */
public class EnviromentManager {
    private static final    String                                                  ENV_DIR_PATH    =System.getProperty("user.home").concat(File.separator).concat(".autotrol").concat(File.separator).concat("APSTPowsyblEditor");
    private static final    String                                                  ENV_FILE_NAME   ="env.yml";
    private static final    String                                                  ENV_FILE_PATH   =ENV_DIR_PATH.concat(File.separator).concat(ENV_FILE_NAME);
    
    private static final    String                                                  ENV_MODULE      ="APST_ENVIROMENT";
    
    public static final     String                                                  PROP_LAST_PATH  ="PROP_LAST_PATH";
    
    
    private final           SimpleMapProperty<String, Map<String,Object>>           props=new SimpleMapProperty<>(FXCollections.observableHashMap());
    
    public EnviromentManager()
    {
        File                            envFile =new File(ENV_FILE_PATH);
        Yaml                            yml     =new Yaml();
        Map<String, Map<String,Object>> parsed  =null;
        try
        {
            Files.createDirectories(Paths.get(ENV_DIR_PATH));
            parsed  =envFile.exists()?(Map<String, Map<String,Object>>)yml.load(new FileInputStream(envFile)):null;
        }
        catch(IOException ex)
        {
        }
        
        if(parsed!=null)
            props.putAll(parsed);
        else
            props.put(ENV_MODULE, FXCollections.observableHashMap());
    }
    
    public void writeEnv() throws IOException
    {
        Files.createDirectories(Paths.get(ENV_DIR_PATH));
        File                            envFile =new File(ENV_FILE_PATH);
        Yaml                            yml     =new Yaml();
        String                          parsed;
//        Map<String, Map<String,Object>> parsed  =envFile.exists()?(Map<String, Map<String,Object>>)yml.load(new FileInputStream(envFile)):null;
        
//        if(parsed!=null)
//            props.putAll(parsed);
        parsed=yml.dumpAsMap(props);
        
        try (FileOutputStream os = new FileOutputStream(envFile)) {
            os.write(parsed.getBytes());
        }
        catch(Exception ex)
        {
            
        }
    }
    
    public void set(String module, String propName, Object value)
    {
        if(!props.containsKey(module))
            props.put(module, FXCollections.observableHashMap());
        props.get().get(module).put(propName, value);
    }
    
    public void set(String propName, Object value)
    {
        set(ENV_MODULE, propName, value);
    }
    
    public Object get(String module, String propName)
    {
        if(!props.containsKey(module))
            return null;
        
        return props.get().get(module).get(propName);
    }
    
    public Object get(String propName)
    {
        if(!props.containsKey(ENV_MODULE))
            return null;
        
        return props.get().get(ENV_MODULE).get(propName);
    }
}
