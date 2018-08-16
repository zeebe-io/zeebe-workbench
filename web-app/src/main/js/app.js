// we use stringify to inline an example XML document
import diagram from '../resources/orderProcess.bpmn';

//make sure you added bpmn-js to your your project
//dependencies via npm install --save bpmn-js
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';

import AnimationModule from "./bpmn-js";

import { queryAll as domQueryAll } from 'min-dom';

import $ from "jquery";

// --- functions

var tests = [];
var currentTest = null;

var openOverlay = '';

function createNewTestCase() {

  const $name = document.getElementById("newTestName");
  const $resource = document.getElementById("newTestResource");
  const $startPayload = document.getElementById("startPayload");

  const $testCases = document.getElementById("testCases");
  const $newTestCase = document.createElement("div");

  $newTestCase.innerHTML = $name.value;
  $newTestCase.classList.add("entry");

  document.getElementById("testCases").childNodes.forEach(c => c.classList.remove("active"));
  $newTestCase.classList.add("active");

  const newTest = {
      name : $name.value,
      resource : null,
      startPayload: $startPayload.value,
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

    document.getElementById("testCases").childNodes.forEach(c => c.classList.remove("active"));
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


      if (activityType == "bpmn:ServiceTask" || activityType == "bpmn:StartEvent" || activityType == "bpmn:EndEvent") {
        canvas.addMarker(activityId, 'diagram-marker');
      }
    });

    eventBus.on("element.out", function(e) {

      const activityId = e.element.id;
      const activityType = e.element.type;

      canvas.removeMarker(activityId, 'diagram-marker');
    });



    eventBus.on("element.click", function(e) {
      // e.element = the model element
      // e.gfx = the graphical element

      if (openOverlay != '') {
        overlays.remove(openOverlay);
      }

      const activityId = e.element.id;
      const activityName = e.element.businessObject.name;
      const activityType = e.element.type;

      const showOverlay = function(activityId, title, onClick) {

        var overlayHtml = $('<div class="diagram-note row">'+
        '<div class="col"><p class="col">' + title + '</p></div>' +
        '<div class="col"><textarea id="command-payload" class="form-control col">{}</textarea></div>' +
        '<div class="col"><button type="button" class="btn btn-light" id="addCommandButton">Done</button></div>' +
        '</div>');

        // attach the overlayHtml to a node
        openOverlay = overlays.add(activityId, {
          position: {
            bottom: 0,
            right: 0
          },
          html: overlayHtml
        });

        document.getElementById("addCommandButton")
                .addEventListener("click",() => {

                  const $payload = document.getElementById("command-payload");
                  onClick($payload.value);

                  overlays.remove(openOverlay);
                  openOverlay = '';
                });
      }

      if (activityType == "bpmn:ServiceTask") {

        showOverlay(activityId, "Complete with payload:", payload => {
          currentTest.commands.push({
            activityId: activityId,
            payload: payload,
          });

          renderTestCase(currentTest);
        });

      } else if (activityType == "bpmn:StartEvent") {

        showOverlay(activityId, "Create instance with payload:", payload => {
          currentTest.startPayload = payload;
          renderTestCase(currentTest);
        });

      } else if (activityType == "bpmn:EndEvent") {

      showOverlay(activityId, "Verify completed with payload:", payload => {

        currentTest.verifications.push({
          activityId: activityId,
          expectedPayload: payload,
          expectedIntent: "END_EVENT_OCCURRED"
        });

        renderTestCase(currentTest);
      });
    }

    });

    renderTestCase(testCase);
  });
}

function renderTestCase(testCase) {
  const $commands = document.getElementById("commands");

  while ($commands.firstChild) {
    $commands.removeChild($commands.firstChild);
  }

  if (testCase.startPayload) {
    const $newCommand = document.createElement("div");

    $newCommand.innerHTML = "Create workflow instance with payload: " + testCase.startPayload;
    $commands.appendChild($newCommand);
  }

  testCase.commands.forEach(cmd => {
    const $newCommand = document.createElement("div");

    $newCommand.innerHTML = "Complete Job '" + cmd.activityId + "' with payload: " + cmd.payload;
    $commands.appendChild($newCommand);
  });

  testCase.verifications.forEach(v => {
    const $newVerification = document.createElement("div");

    $newVerification.innerHTML = "Verify '" + v.activityId + "' is completed with payload: " + v.expectedPayload;
    $commands.appendChild($newVerification);
  });
}

function runTestCases(tests) {

  $.ajax({
        type : 'POST',
        url: '/run',
        data:  JSON.stringify(tests),
        contentType: 'application/json; charset=utf-8',
        success: function (result) {
        	showTestResults(result);
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

function showTestResults(results) {
  const $testRuns = document.getElementById("testRuns");
  const $testFailures = document.getElementById("testFailures");
  const $progressSuccess = document.getElementById("testResultProgressSuccess");
  const $progressFailures = document.getElementById("testResultProgressFailures");

  const runs = results.length;
  const failures = results.filter(r => r.failedVerifications.length > 0).length;

  $testRuns.innerHTML = runs;
  $testFailures.innerHTML = failures;

  $progressSuccess.style.width = 100 * (1 - (failures / runs)) + "%";
  $progressFailures.style.width = 100 * (failures / runs) + "%";
}

// ---

var viewer = new BpmnViewer({
  container: '#canvas',
  additionalModules: [
    AnimationModule
  ]
});

window.viewer = viewer;
window.tests = tests;
