#IIDM-Protobuf conversion

## Protobuf schema to java
protoc -I=. --java_out=./src/main/java ./src/main/resources/proto/iidm.proto

## Xsd2thrift experiments
Xsd2thrift (https://github.com/tranchis/xsd2thrift) allegedly converts an xml schema to a proto file
  java -jar xsd2thrift-1.0.jar --protobuf --filename=iidm.proto --package=com.powsybl.iidm.protobuf.proto --splitBySchema=true iidm.xsd

Once you have a .proto file, you can produce a .java file, using the related protobuf tool 'protoc'
Unfortunately though, the code contains a lot of errors and can't be used 'out of the box'


