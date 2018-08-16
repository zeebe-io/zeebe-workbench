// we use stringify to inline an example XML document
import diagram from '../resources/orderProcess.bpmn';

//make sure you added bpmn-js to your your project
//dependencies via npm install --save bpmn-js
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';

import AnimationModule from "./bpmn-js";

import { queryAll as domQueryAll } from 'min-dom';

import $ from "jquery";

const history = [
  { activityId: 'order-placed' },   // start event
  { activityId: 'SequenceFlow_1ml1so0' },  // outgoing sequence flow
  { activityId: 'collect-money' }    // activityId
]

// --- functions

function renderHistory(animation, elementRegistry, tokenCount) {
  const $history = document.getElementById("history");

  $history.innerHTML = '';

  history.forEach(activity => {
    const element = elementRegistry.get(activity.activityId);

    const $entry = document.createElement("div");

    const type = element.type.split(":")[1];

    $entry.innerHTML = activity.activityId + " (" + type + ")";
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

var tests = [];
var currentTest = null;

var openOverlay = '';

function createNewTestCase() {

  const $name = document.getElementById("newTestName");
  const $resource = document.getElementById("newTestResource");

  const $testCases = document.getElementById("testCases");
  const $newTestCase = document.createElement("div");

  $newTestCase.innerHTML = $name.value;
  $newTestCase.classList.add("entry");
  $newTestCase.classList.add("active");

  const newTest = {
      name : $name.value,
      resource : null,
      commands: [],
      verifications: []
  };

  var file = $resource.files[0];

  var reader = new FileReader();
  reader.onload = function(e) {

		            var binary = '';
		            var bytes = new Uint8Array( e.target.result );
		            var len = bytes.byteLength;
		            for (var j = 0; j < len; j++) {
		                binary += String.fromCharCode( bytes[ j ] );
		            }

                currentTest.resource = {
                  name: file.name,
                  xml: binary,
		            	content:  btoa(binary)
                };


                showTestCase(newTest);
	};
  reader.readAsArrayBuffer(file);

  $newTestCase.addEventListener("click", () => {
    const $entries = domQueryAll("#testCases.entry");
    $entries.forEach($e => $e.classList.remove("active"));

    $newTestCase.classList.add("active");

    currentTest = newTest;
    showTestCase(newTest);
  });

  $testCases.appendChild($newTestCase);

  tests.push(newTest);
  currentTest = newTest;
}

document.getElementById("createTestCaseButton")
        .addEventListener("click", () => createNewTestCase());

function showTestCase(testCase) {

  viewer.importXML(testCase.resource.xml, function(err) {

   if (!err) {
      viewer.get('canvas').zoom('fit-viewport');
    } else {
      console.log('something went wrong:', err);
    }

    const canvas = viewer.get("canvas"),
          elementRegistry = viewer.get("elementRegistry"),
          animation = viewer.get("animation"),
          tokenCount = viewer.get("tokenCount"),
          eventBus = viewer.get("eventBus"),
          overlays = viewer.get("overlays");

    canvas.zoom('fit-viewport');

    eventBus.on("element.hover", function(e) {

      const activityId = e.element.id;
      const activityType = e.element.type;


      if (activityType == "bpmn:ServiceTask") {
        canvas.addMarker(activityId, 'diagram-marker');
      }
    });

    eventBus.on("element.out", function(e) {

      const activityId = e.element.id;
      const activityType = e.element.type;

      if (activityType == "bpmn:ServiceTask") {
        canvas.removeMarker(activityId, 'diagram-marker');
      }
    });



    eventBus.on("element.click", function(e) {
      // e.element = the model element
      // e.gfx = the graphical element

      const activityId = e.element.id;
      const activityName = e.element.businessObject.name;
      const activityType = e.element.type;

      if (activityType != "bpmn:ServiceTask") {
        return;
      }

      var overlayHtml = $('<div class="diagram-note row">'+
      '<div class="col"><p class="col">Complete with payload:</p></div>' +
      '<div class="col"><textarea id="job-payload" class="form-control col">{}</textarea></div>' +
      '<div class="col"><button type="button" class="btn btn-light" id="addCommandButton">Add</button></div>' +
      '</div>');

      if (openOverlay != '') {
        overlays.remove(openOverlay);
      }

      // attach the overlayHtml to a node
      openOverlay = overlays.add(activityId, {
        position: {
          bottom: 0,
          right: 0
        },
        html: overlayHtml
      });

      document.getElementById("addCommandButton")
              .addEventListener("click", function(e) {
                const $payload = document.getElementById("job-payload");
                createNewCommand(activityId, $payload.value)

                overlays.remove(openOverlay);
                openOverlay = '';
              });


    });

    renderTestCase(testCase);
  });
}

function renderTestCase(testCase) {
  const $commands = document.getElementById("commands");

  while ($commands.firstChild) {
    $commands.removeChild($commands.firstChild);
  }

  testCase.commands.forEach(cmd => {
    const $newCommand = document.createElement("div");

    $newCommand.innerHTML = "Complete Job '" + cmd.activityId + "' with Payload: " + cmd.payload;
    $commands.appendChild($newCommand);
  });
}

function createNewCommand(activityId, payload) {

  currentTest.commands.push({
    activityId: activityId,
    payload: payload,
  })

  renderTestCase(currentTest);
}

function runTestCases(tests) {

  $.ajax({
        type : 'POST',
        url: '/run',
        data:  JSON.stringify(tests),
        contentType: 'application/json; charset=utf-8',
        success: function (result) {
        	console.log("TODO show result: " + result);
        },
        error: function (xhr, ajaxOptions, thrownError) {
       	 console.log("failed to run tests: " + thrownError);
        },
   	 timeout: 60000,
     crossDomain: true,
	});
};

document.getElementById("runTestsButton")
        .addEventListener("click", () => runTestCases(tests));

// ---

var viewer = new BpmnViewer({
  container: '#canvas',
  additionalModules: [
    AnimationModule
  ]
});


window.viewer = viewer;

window.tests = tests;
