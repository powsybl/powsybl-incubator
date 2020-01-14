#IIDM-AVRO conversion
IIDM to [Apache AVRO](https://avro.apache.org/) converter


## xsd to AVRO schema
[xml-avro](https://github.com/GeethanadhP/xml-avro) used to create the IIDM AVRO schema, from the XIIDM XSD schema (iidm.xsd).

    cat <<EOF > iidm-avro-config.yml
    baseDir: "."
    debug: true
    namespaces: true
    XSD:
      xsdFile: "iidm.xsd"
      avscFile: "iidm.avsc"
      stringTimestamp: true
    EOF
    java -jar ./xml-avro-all-<VERSION>.jar -c ./iidm-avro-config.yml


