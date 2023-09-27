const path = require('path');

module.exports = {
  entry: './src/index.js',
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'main.js',
    libraryTarget: 'umd',
    library: 'NetworkMapViewer',
    umdNamedDefine: true
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env', '@babel/preset-react']
          }
        }
      },
      {
        test: /\.svg$/,
        use: ['@svgr/webpack'],
      }
    ]
  },
  resolve: {
    //XXX why is this needed ?
    // ERROR in ./node_modules/react-map-gl/dist/esm/exports-mapbox.js 16:15-34
    // Module not found: Error: Can't resolve 'mapbox-gl' in '/home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/react-map-gl/dist/esm'
    // resolve 'mapbox-gl' in '/home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/react-map-gl/dist/esm'
    //   Parsed request is a module
    //   using description file: /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/react-map-gl/package.json (relative path: ./dist/esm)
    //     Field 'browser' doesn't contain a valid alias configuration
    //     resolve as module
    //       /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/react-map-gl/dist/esm/node_modules doesn't exist or is not a directory
    //       /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/react-map-gl/dist/node_modules doesn't exist or is not a directory
    //       /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/react-map-gl/node_modules doesn't exist or is not a directory
    //       /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/node_modules doesn't exist or is not a directory
    //       looking for modules in /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules
    //         single file module
    //           using description file: /home/harperjon/Projects/powsybl-incubator/network-map-viewer/package.json (relative path: ./node_modules/mapbox-gl)
    //             no extension
    //               Field 'browser' doesn't contain a valid alias configuration
    //               /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/mapbox-gl doesn't exist
    //             .js
    //               Field 'browser' doesn't contain a valid alias configuration
    //               /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/mapbox-gl.js doesn't exist
    //             .jsx
    //               Field 'browser' doesn't contain a valid alias configuration
    //               /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/mapbox-gl.jsx doesn't exist
    //         /home/harperjon/Projects/powsybl-incubator/network-map-viewer/node_modules/mapbox-gl doesn't exist
    //       /home/harperjon/Projects/powsybl-incubator/node_modules doesn't exist or is not a directory
    //       /home/harperjon/Projects/node_modules doesn't exist or is not a directory
    //       /home/harperjon/node_modules doesn't exist or is not a directory
    //       /home/node_modules doesn't exist or is not a directory
    //       /node_modules doesn't exist or is not a directory
    //  @ ./node_modules/react-map-gl/dist/esm/index.js 1:0-33 1:0-33 2:0-54 2:0-54
    //  @ ./src/components/network/network-map.js 20:0-54 471:22-25
    //  @ ./src/index.js 4:0-73 4:0-73
    alias: {
      'mapbox-gl': 'maplibre-gl'
    },
    extensions: ['.js', '.jsx']
  },
  externals: [
    'react',
    'react-dom',
    'react-intl',
    /^@mui\/.+$/
  ],
  plugins: [
  ]
};
