#IIDM - binary converters
iidm-avro-converter and iidm-protobuf-converter modules provide a couple of IIDM prototype converters to/from
Protocol Buffer(Protobuf) and AVRO binary serialization formats.


## XIIDM-Protobuf-AVRO converters performances comparison
To start testing and comparing the two binary converters performances with XIIDM (serialization/deserialization time and the resulting files sizes)
a reference 24M XIIDM file was used (not included in the project): a bus-breaker xiidm network - with no iidm extensions, to align to the current binary converters limitations -
that counts more than 5000 substations, 6000 voltage levels, 2600 generators, 8500 lines, 8000 loads, 2000 transformers. 
Here are the results:

| Format | Import/export | Time(ms) | File size | Notes | 
| --------------- | --------------- | ---------------- | --------------- | --------------- |
| XML xiidm | Export  | 2535  | 24587610  | 2.3M, when gzipped  |
| XML xiidm| Import  | 2312  | 24587610  |   |
| Protobuf | Export  | 1757  | 11240267 | 2.6M, when gzipped  |
| Protobuf  | Import  | 1403  | 11240267  |   |
| AVRO | Export  | 2467  | 10983200 | 2.4M, when gzipped  |
| AVRO | Import  | 2702  | 10983200  |   |


