// we use stringify to inline an example XML document
import pizzaDiagram from '../resources/orderProcess.bpmn';


const history = [
  { activitId: 'order-placed' },   // start event
  { activitId: 'SequenceFlow_1ml1so0' },  // outgoing sequence flow
  { activitId: 'collect-money' }    // activitId
]

import { queryAll as domQueryAll } from 'min-dom';

function renderHistory(animation, elementRegistry, tokenCount) {
  const $history = document.getElementById("history");

  $history.innerHTML = '';

  history.forEach(activity => {
    const element = elementRegistry.get(activity.activitId);

    const $entry = document.createElement("div");

    const type = element.type.split(":")[1];

    $entry.innerHTML = activity.activitId + " (" + type + ")";
    $entry.classList.add("entry");

    if (type === "SequenceFlow") {
        $entry.addEventListener("click", () => {
          animation.createAnimation(element, "", () => { console.log("animation done") });
        });
    } else {

      $entry.addEventListener("click", () => {
        elementRegistry.forEach(element => {
          tokenCount.removeTokenCount(element);
        });

        elementRegistry.forEach(element => {
          if (!element.waypoint && Math.random() > 0.7) {
            tokenCount.addTokenCount(element, Math.round(Math.random() * 50));
          }
        });
      });

    }

    $entry.addEventListener("click", () => {
      const $entries = domQueryAll("#history.entry");

      $entries.forEach($e => $e.classList.remove("active"));

      $entry.classList.add("active");
    });

    $history.appendChild($entry);
  });
}

// make sure you added bpmn-js to your your project
// dependencies via npm install --save bpmn-js
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';

import AnimationModule from "./custom";

var viewer = new BpmnViewer({
  container: '#canvas',
  additionalModules: [
    AnimationModule
  ]
});

viewer.importXML(pizzaDiagram, function(err) {


 if (!err) {
    console.log('success!');
    viewer.get('canvas').zoom('fit-viewport');
  } else {
    console.log('something went wrong:', err);
  }

  const canvas = viewer.get("canvas"),
        elementRegistry = viewer.get("elementRegistry"),
        animation = viewer.get("animation"),
        tokenCount = viewer.get("tokenCount");

  canvas.zoom('fit-viewport');

  renderHistory(animation, elementRegistry, tokenCount);

});

window.viewer = viewer;
