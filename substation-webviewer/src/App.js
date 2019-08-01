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
    var metadata = fetch("substation_metadata.json")
      .then(res => res.json());

    // TODO sanitize svg, remove javscript, references to other svg etc.
    // impossible to do it ourselves, need a lib
    var svg = fetch("substation.svg")
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

  // Data was loaded
  componentDidUpdate() {
     if (this.state.error) {
       return;
     }
     var draw = SVG().addTo('#drawing').size(1800, 800).viewbox(0,0,1800,800).panZoom({zoomMin: 0.5, zoomMax: 10, zoomFactor:0.2});
     draw.svg(this.state.svgstr);

     // TODO: Do we have to write this ourselves ? Or does it exist in
        // sgvdotjs
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
     var initialWiresPoints = {};

     forEachChild(draw, e => { if (e.node && e.node.id) { childrenById[e.node.id] = e}
     console.log("CHILD " + e);
     if (this.state.wiresById[e.node.id]) {
         initialWiresPoints[e.node.id] = e.array();
         console.log("its a wire " + e.node.id + " " + e.array());
     }});


     function getAnchorPoint(node, idx, rotated) {

        var x;
        var y;
        var orientation;
        if (rotated) {
            x = node.anchorPoints[idx].y;
            y =node.anchorPoints[idx].x;
            if (node.anchorPoints[idx].orientation === 'HORIZONTAL') {
                orientation = 'VERTICAL';
            } else if (node.anchorPoints[idx].orientation === 'VERTICAL') {
                orientation = 'HORIZONTAL';
            } else {
                orientation = node.anchorPoints[idx].orientation;
            }
            return {'x': x, 'y': y, 'orientation': orientation };
        } else {
            return node.anchorPoints[idx];
        }

     }

     function untransform(group) {
         group.untransform();
         for (var i =0; i<group.children().length;i++) {
             group.children()[i].untransform();
         }
     }

     function   relocateArrow(arrow, points, size, distance) {

         const finalPos = positionAtDistance(points, distance);
         console.log("finalPos (" + finalPos['x'] + ' , ' + finalPos['y'] + ') ' + finalPos['angle']);
         untransform(arrow);
         arrow.move(finalPos['x'] - size / 2, finalPos['y'] - size / 2);
         if (arrow.children) {
             var c = arrow.children();
             for (var i =0; i<c.length;i++) {
                 if (c[i].id().startsWith('SvgjsText')){
                     if (finalPos['angle'] != 180)  {
                         c[i].rotate(finalPos['angle'],finalPos['x'], finalPos['y']);
                     }
                 } else {
                     c[i].rotate(finalPos['angle']);
                 }
             }
         }
     }

     function positionAtDistance( points, distance) {
         var residual = distance;

         var px1;
         var py1;
         var px2;
         var py2;
         for (var i = 0; i < points.length - 1; i++) {
             px1 = points[i][0];
             py1 = points[i][1];
             px2 = points[i + 1][0];
             py2 = points[i + 1][1];

             const  segmentLength = Math.sqrt((px2-px1) * (px2-px1)  + (py2 - py1) * (py2 - py1));
             if (segmentLength < residual) {
                 residual -= segmentLength;
             } else {
                 return positionAtRatio(px1, py1, px2, py2, residual / segmentLength);
             }
         }

         return positionAtRatio(px1, py1, px2, py2, 1);
     }

     function positionAtRatio(x1, y1, x2, y2, ratio) {
         const x = (x2 - x1) * ratio;
         const y = (y2 - y1) * ratio;
         var res = [];
         res['x'] = x1 + x;
         res['y'] = y1 + y;
         res['angle'] = getAngle(0, 1, x, y);
         return res;

     }

     function getAngle(x1, y1, x2, y2) {
         return 180 * Math.atan2(cross(x1, y1, x2, y2), dot(x1, y1, x2, y2)) / Math.PI;
     }

     function dot(x1, y1, x2, y2) {
         return x1 * x2 + y1 * y2;
     }

     function cross(x1, y1, x2, y2) {
         return [x1 * y2 - y1 * x2];
     }

     forEachChild(draw, e=> {
       if ( e.draggable && e.node && e.node.id && this.state.nodesById[e.node.id] ) {
         e.draggable();
         var wires = this.state.metadata.wires.filter( w => w.nodeId1 === e.node.id || w.nodeId2 === e.node.id );
         e.on('dragmove.namespace', event => {
             console.log("dragmove");
           event.preventDefault();// we can't use the default drag because it
                                    // moves the children inside the group
                                    // instead of translating the group
           // Not sure about this computation, I found it empirically to work..
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

             // Compute the absolute position of the dragged node
             var rbox = e.rbox(draw);// Need to compute the transform because
                                        // the wire we want to connect is not in
                                        // the group.
             var groupOffsetX = rbox.x - e.x() + 1;
             var groupOffsetY = rbox.y - e.y() + 1;

             var width = thisComponent.size.width;
             var height = thisComponent.size.height;
             var baseX = groupOffsetX + event.detail.box.x -e.x() + width/2 ;
             var baseY = groupOffsetY + event.detail.box.y -e.y() + height/2;


             // Compute the absolute position of the other node
             var otherrbox = otherObj.rbox(draw);
             var othergroupOffsetX = otherrbox.x - otherObj.x() + 1;
             var othergroupOffsetY = otherrbox.y - otherObj.y() + 1;
             var otherwidth = otherComponent.size.width;
             var otherheight = otherComponent.size.height;
             var otherbaseX = othergroupOffsetX + otherwidth/2 ;
             var otherbaseY = othergroupOffsetY + otherheight/2;
/*
 * if (thisNode.componentType === 'NODE') { baseX = groupOffsetX - width/2;
 * baseY = groupOffsetY + height/2; } if (otherNode.componentType === 'NODE') {
 * otherbaseX = othergroupOffsetX - otherwidth/2; otherbaseY = othergroupOffsetY +
 * otherheight/2; }
 */
             // Find the best anchor points pair
             var mindistsq = +Infinity;
             var anchor_idx;
             var otheranchor_idx;
             for (var a = 0; a<thisComponent.anchorPoints.length; a++) {
               for (var b = 0; b<otherComponent.anchorPoints.length; b++) {
                  var a1 = getAnchorPoint(thisComponent, a, thisNode.rotated);
                  var anchorx = a1.x; // thisComponent.anchorPoints[a].x;
                  var anchory = a1.y; // thisComponent.anchorPoints[a].y;
                  var a2 = getAnchorPoint(otherComponent, b, otherNode.rotated);
                  var otheranchorx = a2.x; // otherComponent.anchorPoints[b].x;
                  var otheranchory = a2.y; // otherComponent.anchorPoints[b].y;
                  var newdist = ((baseX + anchorx) - (otherbaseX + otheranchorx)) * ((baseX + anchorx) - (otherbaseX + otheranchorx)) +
                                ((baseY + anchory) - (otherbaseY + otheranchory)) * ((baseY + anchory) - (otherbaseY + otheranchory))
                  if (newdist < mindistsq) {
                    anchor_idx = a;
                    otheranchor_idx = b;
                    mindistsq = newdist;
                  }
               }
             }

             // Draw the wire
             var finalanchor = getAnchorPoint(thisComponent, anchor_idx, thisNode.rotated);
             var finalanchorx = finalanchor.x;
             var finalanchory = finalanchor.y;
             var x1 = baseX + finalanchorx;
             var y1 = baseY + finalanchory;
             var finalotheranchor = getAnchorPoint(otherComponent, otheranchor_idx, otherNode.rotated);

             var finalotheranchorx = finalotheranchor.x;
             var finalotheranchory = finalotheranchor.y;
             var x2 = otherbaseX + finalotheranchorx;
             var y2 = otherbaseY + finalotheranchory;


             if (x1 === x2 || y1 === y2) {
               wireObj.plot([[x1, y1], [x2, y2]]);
             } else {

                if ( finalanchor.orientation === "VERTICAL" ) {
                    if ( finalotheranchor.orientation === "VERTICAL" ) {
                    wireObj.plot([[x1, y1], [x1, (y1 + y2) / 2], [x2, (y1 + y2) / 2], [x2, y2]]);
                  } else {
                    wireObj.plot([[x1, y1], [x1, y2], [x2, y2]]);
                  }
                } else {
                  if ( finalotheranchor.orientation === "HORIZONTAL" ) {
                    wireObj.plot([[x1, y1], [(x1 + x2) / 2, y1], [(x1 + x2) / 2, y2], [x2, y2]]);
                  } else {
                    wireObj.plot([[x1, y1], [x2, y1], [x2, y2]]);
                  }
                }
             }

             var wireArrows = this.state.metadata.arrows.filter( a => a.wireId === wire.id).sort((a,b) => (a.id > b.id) ? 1 : ((b.id > a.id) ? -1 : 0));

             var arrowSize = componentsByType['ARROW'].size.height;
             for (var i = 0; i< wireArrows.length; i++) {
                 relocateArrow(childrenById[wireArrows[i].id], wireObj.array(), arrowSize, (2 +  2 * i) * arrowSize);
             }

           });
           // Drag the element

           e.translate(event.detail.box.x -e.x(), event.detail.box.y -e.y());
           // this is what the drag does normally by moving, do it ourselves by
            // translating

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
