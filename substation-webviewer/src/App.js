import React, { Component } from 'react';
import SVG from "@svgdotjs/svg.js/src/svg.js";
import "@svgdotjs/svg.draggable.js";
import "@svgdotjs/svg.panzoom.js";
import './App.css';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
    };
  }

  componentDidMount() {
    var metadata = fetch("test_metadata.json")
      .then(res => res.json());

    //TODO sanitize svg, remove javscript, references to other svg etc.
    //impossible to do it ourselves, need a lib
    var svg = fetch("test.svg")
      .then(res => res.text());

    Promise.all([metadata, svg])
      .then(
        (result) => {
          var metadata = result[0];
          var svgstr = result[1];
          const makeDict = (arr, field) => Object.assign({}, ...arr.map(e => ({[e[field]]: e})))
          var componentsByType = makeDict(metadata.components, "type");
          const makeIdDict = (arr, field) => makeDict(arr, "id");
          var nodesById = makeIdDict(metadata.nodes);
          var componentsById = makeIdDict(metadata.components);
          var wiresById = makeIdDict(metadata.wires);
          this.setState({
            error: null,
            isLoaded: true,
            nodesById,
            wiresById,
            componentsById,
            componentsByType,
            metadata,
            svgstr,
          });
        },
        // Note: it's important to handle errors here
        // instead of a catch() block so that we don't swallow
        // exceptions from actual bugs in components.
        (error) => {
          this.setState({
            isLoaded: true,
            error
          });
        }
      )
  }

  //Data was loaded
  componentDidUpdate() {
     if (this.state.error) {
       return;
     }
     var draw = SVG().addTo('#drawing').size(800, 800).viewbox(0,0,800,800).panZoom({zoomMin: 0.5, zoomMax: 10, zoomFactor:0.2});
     draw.svg(this.state.svgstr);

     //TODO: Do we have to write this ourselves ? Or does it exist in sgvdotjs
     function forEachChild(mynode, mapper) {
        mapper(mynode);
        if (mynode.children) {
            var c = mynode.children();
            for (var i =0; i<c.length;i++) {
                forEachChild(c[i], mapper);
            }
        }
     }
     var childrenById = {};
     forEachChild(draw, e => { if (e.node && e.node.id) { childrenById[e.node.id] = e }});

     forEachChild(draw, e=> {
       if ( e.draggable && e.node && e.node.id && this.state.nodesById[e.node.id] ) {
         e.draggable();
         var wires = this.state.metadata.wires.filter( w => w.nodeId1 === e.node.id || w.nodeId2 === e.node.id );
         e.on('dragmove.namespace', event => {
           event.preventDefault();//we can't use the default drag because it moves the children inside the group instead of translating the group

           //Not sure about this computation, I found it empirically to work..
           wires.forEach( wire => {
             var wiresById = this.state.wiresById;
             var nodesById = this.state.nodesById;
             var componentsByType = this.state.componentsByType;
             var thisNode = nodesById[e.node.id];
             var thisComponent = componentsByType[thisNode.componentType];

             var wireObj = childrenById[wire.id];
             var otherId = wiresById[wire.id].nodeId1 === e.node.id ? wiresById[wire.id].nodeId2 : wiresById[wire.id].nodeId1;
             var otherObj = childrenById[otherId];

             var otherNode = nodesById[otherId];
             var otherComponent = componentsByType[otherNode.componentType];

             //Compute the absolute position of the dragged node
             var rbox = e.rbox(draw);//Need to compute the transform because the wire we want to connect is not in the group.
             var groupOffsetX = rbox.x - e.x() + 1;
             var groupOffsetY = rbox.y - e.y() + 1;
             var width = thisComponent.size.width;
             var height = thisComponent.size.height;
             var baseX = groupOffsetX + event.detail.box.x -e.x() + width/2 ;
             var baseY = groupOffsetY + event.detail.box.y -e.y() + height/2;

             //Compute the absolute position of the other node
             var otherrbox = otherObj.rbox(draw);
             var othergroupOffsetX = otherrbox.x - otherObj.x() + 1;
             var othergroupOffsetY = otherrbox.y - otherObj.y() + 1;
             var otherwidth = otherComponent.size.width;
             var otherheight = otherComponent.size.height;
             var otherbaseX = othergroupOffsetX + otherwidth/2 ;
             var otherbaseY = othergroupOffsetY + otherheight/2;

             //Find the best anchor points pair
             var mindistsq = +Infinity;
             var anchor_idx;
             var otheranchor_idx;
             for (var a = 0; a<thisComponent.anchorPoints.length; a++) {
               for (var b = 0; b<otherComponent.anchorPoints.length; b++) {
                  var anchorx = thisComponent.anchorPoints[a].x;
                  var anchory = thisComponent.anchorPoints[a].y;
                  var otheranchorx = otherComponent.anchorPoints[b].x;
                  var otheranchory = otherComponent.anchorPoints[b].y;
                  var newdist = ((baseX + anchorx) - (otherbaseX + otheranchorx)) * ((baseX + anchorx) - (otherbaseX + otheranchorx)) +
                                ((baseY + anchory) - (otherbaseY + otheranchory)) * ((baseY + anchory) - (otherbaseY + otheranchory))
                  if (newdist < mindistsq) {
                    anchor_idx = a;
                    otheranchor_idx = b;
                    mindistsq = newdist;
                  }
               }
             }

             //Draw the wire
             var finalanchorx = thisComponent.anchorPoints[anchor_idx].x;
             var finalanchory = thisComponent.anchorPoints[anchor_idx].y;
             var x1 = baseX + finalanchorx;
             var y1 = baseY + finalanchory;
             var finalotheranchorx = otherComponent.anchorPoints[otheranchor_idx].x;
             var finalotheranchory = otherComponent.anchorPoints[otheranchor_idx].y;
             var x2 = otherbaseX + finalotheranchorx;
             var y2 = otherbaseY + finalotheranchory;
             if (x1 === x2 || y1 === y2) {
               wireObj.plot([[x1, y1], [x2, y2]]);
             } else {
                if ( thisComponent.anchorPoints[anchor_idx].orientation === "VERTICAL" ) {
                  if ( otherComponent.anchorPoints[otheranchor_idx].orientation === "VERTICAL" ) {
                    wireObj.plot([[x1, y1], [x1, (y1 + y2) / 2], [x2, (y1 + y2) / 2], [x2, y2]]);
                  } else {
                    wireObj.plot([[x1, y1], [x1, y2], [x2, y2]]);
                  }
                } else {
                  if ( otherComponent.anchorPoints[otheranchor_idx].orientation === "HORIZONTAL" ) {
                    wireObj.plot([[x1, y1], [(x1 + x2) / 2, y1], [(x1 + x2) / 2, y2], [x2, y2]]);
                  } else {
                    wireObj.plot([[x2, y2], [x2, y1], [x1, y1]]);
                  }
                }
             }
           });
           //Drag the element
           e.translate(event.detail.box.x -e.x(), event.detail.box.y -e.y());//this is what the drag does normally by moving, do it ourselves by translating
         })
       }
     });
  }

  render() {
    const error = this.state.error
    if (error) {
      return <p>{error.toString()}</p>;
    } else {
      return <div id="drawing"/>
    }
  }
}

export default App;
