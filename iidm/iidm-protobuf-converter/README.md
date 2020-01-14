#IIDM-Protobuf conversion


## Xsd2thrift experiments
Xsd2thrift (https://github.com/tranchis/xsd2thrift) allegedly converts an xml schema to a proto file

  java -jar xsd2thrift-1.0.jar --protobuf --filename=iidm.proto --package=com.powsybl.iidm.protobuf.proto --splitBySchema=true iidm.xsd

Once you have a .proto file, you can compile it to java (via protobuf compiler).
Apparently though, this java code contains a lot of errors and can't be used 'out of the box'; Therefore it is better to create .proto definitions from scratch.



