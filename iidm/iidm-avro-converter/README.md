#IIDM-AVRO converter
IIDM to [Apache AVRO](https://avro.apache.org/) converter.
This module allows serializing an IIDM network to a binary AVRO file (with extension .aiidm)

Present time limitations:
* Node-breaker network are not supported (Bus-Breaker, only).
* IIDM extensions are not supported.


## AVRO schema
[xml-avro](https://github.com/GeethanadhP/xml-avro) has been used to create the IIDM AVRO schema, from the XIIDM XSD schema (iidm.xsd).

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


