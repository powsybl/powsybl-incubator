# Network-map-viewer
Library for a react component showing a network (substations, voltage levels, lines) over a map.

To build the library:
```console
npm run build
```

# Demo
The library includes a demo react app, in the demo directory (the network-map-viewer component is instantiated in demo/src/App.js).

To run the demo:
```console
cd demo
npm run build
npm start
```
and point the browser to http://localhost:3000

###  Notes
Component's code based on Deck.gl and Mapbox-gl, extracted from the [gridstudy-app](https://github.com/gridsuite/gridstudy-app) project.