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
var testResults = [];

var openOverlay = '';

window.newTestCase = function() {
  const $name = document.getElementById("newTestName");
  const $resource = document.getElementById("newTestResource");
  const $startPayload = document.getElementById("startPayload");

  const $testCases = document.getElementById("testCases");
  const $tr = document.createElement("tr");

  $tr.innerHTML = "<td></td>" + // status
                  "<td>" + $name.value + "</td>";

  $tr.testName = $name.value;


  $("#testCases>tr.table-active").removeClass("table-active");
  $tr.classList.add("table-active");

  $testCases.appendChild($tr);

  const newTest = {
      name : $name.value,
      resource : null,
      startPayload: $startPayload.value,
      commands: [],
      verifications: []
  };

  currentTest = newTest;
  tests.push(newTest);

  $tr.addEventListener("click", () => {

    $("#testCases>tr.table-active").removeClass("table-active");
    $tr.classList.add("table-active");

    currentTest = newTest;
    showTestCase(newTest);
  });

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


}

window.removeTestCase = function() {
    const index = tests.indexOf(currentTest);
    tests.pop(index);

    const $testCases = document.getElementById("testCases");
    const $tr = $testCases.childNodes[index];
    $testCases.removeChild($tr);

    if (tests.length > 0) {
      const selectedTest = (index - 1) % tests.length;
      currentTest = tests[selectedTest];

      $testCases.childNodes[selectedTest].classList.add("table-active");
      showTestCase(currentTest);
    } else {
      const $testName = document.getElementById("test-name");
      $testName.innerHTML = "";

      const $commands = document.getElementById("commands");
      while ($commands.firstChild) {
        $commands.removeChild($commands.firstChild);
      }

      viewer.detach();
    }
  }

window.runSingleTest = function() {
    runTestCases([currentTest]);
}

function showTestCase(testCase) {

  viewer.importXML(testCase.resource.xml, function(err) {

   if (!err) {
      viewer.attachTo(document.getElementById("canvas"));
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

    window.elementRegistry = elementRegistry;
    window.tokenCount = tokenCount;

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

      const showOverlay = function(activityId, title, payload, onClick) {

        var overlayHtml = $('<div class="diagram-note row">'+
        '<div class="col"><p class="col">' + title + '</p></div>' +
        '<div class="col"><textarea id="command-payload" class="form-control col">' + payload + '</textarea></div>' +
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

        var command = null;
        if (testCase.commands.filter(c => c.activityId == activityId).length > 0) {
          command = testCase.commands.filter(c => c.activityId == activityId)[0];
        } else {
          command = {
            activityId: activityId,
            payload: "{}"
          };
          testCase.commands.push(command);
        }

        showOverlay(activityId, "Complete with payload:", command.payload, payload => {
          command.payload = payload;

          renderTestCase(currentTest);
        });

      } else if (activityType == "bpmn:StartEvent") {

        showOverlay(activityId, "Create instance with payload:", currentTest.startPayload, payload => {
          testCase.startPayload = payload;
          renderTestCase(currentTest);
        });

      } else if (activityType == "bpmn:EndEvent") {

        var verification = null;
        if (testCase.verifications.filter(v => v.activityId == activityId).length > 0) {
          verification = testCase.verifications.filter(v => v.activityId == activityId)[0];
        } else {
          verification = {
            activityId: activityId,
            expectedPayload: "{}",
            expectedIntent: "END_EVENT_OCCURRED"
          };
          testCase.verifications.push(verification);
        }

      showOverlay(activityId, "Verify completed with payload:", verification.expectedPayload, payload => {
        verification.expectedPayload = payload;
        renderTestCase(currentTest);
      });
    }

    });

    renderTestCase(testCase);
  });

  renderTestCase(testCase);
}

function renderTestCase(testCase) {

  const $testName = document.getElementById("test-name");
  $testName.innerHTML = testCase.name;

  const $commands = document.getElementById("commands");

  while ($commands.firstChild) {
    $commands.removeChild($commands.firstChild);
  }

  if (testCase.startPayload) {
    const $newCommand = document.createElement("li");

    $newCommand.innerHTML = "Create workflow instance with payload: " + testCase.startPayload;
    $newCommand.classList.add("list-group-item");
    $commands.appendChild($newCommand);
  }

  testCase.commands.forEach(cmd => {
    const $newCommand = document.createElement("li");

    $newCommand.innerHTML = "Complete Job '" + cmd.activityId + "' with payload: " + cmd.payload;
    $newCommand.classList.add("list-group-item");
    $commands.appendChild($newCommand);
  });

  testCase.verifications.forEach(v => {
    const $newVerification = document.createElement("li");

    $newVerification.innerHTML = "Verify '" + v.activityId + "' is completed with payload: " + v.expectedPayload;

    if (window.testResults.filter(r => r.name == testCase.name).length > 0) {
      const result = window.testResults.filter(r => r.name == testCase.name)[0];

      $newVerification.classList.remove("list-group-item-success");
      $newVerification.classList.remove("list-group-item-danger");

      if (result.failedVerifications.filter(f => f.activityId == v.activityId).length > 0) {

        const failedVerification = result.failedVerifications.filter(f => f.activityId == v.activityId)[0];

        $newVerification.classList.add("list-group-item-danger");

        const $failures = document.createElement("ul");
        const $failure = document.createElement("li");

        if (failedVerification.actualPayload) {
          $failure.innerHTML = "Payload was " + failedVerification.actualPayload;
        } else {
          $failure.innerHTML = "Activity was not completed";
        }

        $failures.appendChild($failure);
        $newVerification.appendChild($failures);

      } else {
        $newVerification.classList.add("list-group-item-success");
      }
    }

    $newVerification.classList.add("list-group-item");
    $commands.appendChild($newVerification);
  });
}

window.runAllTests = function () {
  runTestCases(tests)
}

function runTestCases(tests) {

  const $testRuns = document.getElementById("testRuns");
  const $testFailures = document.getElementById("testFailures");

  $testRuns.innerHTML = "0";
  $testFailures.innerHTML = "0";

  const $progressSuccess = document.getElementById("testResultProgressSuccess");
  const $progressFailures = document.getElementById("testResultProgressFailures");

  $progressSuccess.style.width = "100%";
  $progressFailures.style.width = "0%";

  $progressSuccess.classList.add("progress-bar-striped");
  $progressSuccess.classList.add("progress-bar-animated");

  document.getElementById("testCases").childNodes.forEach(c => {
      const $status = c.firstChild;

      $status.innerHTML = "";
      $status.classList.remove("successful-test");
      $status.classList.remove("failed-test");
  });

  $.ajax({
        type : 'POST',
        url: '/run',
        data:  JSON.stringify(tests),
        contentType: 'application/json; charset=utf-8',
        success: function (result) {
          $progressSuccess.classList.remove("progress-bar-striped");
          $progressSuccess.classList.remove("progress-bar-animated");

        	showTestResults(result);
        },
        error: function (xhr, ajaxOptions, thrownError) {
       	 console.log("failed to run tests: " + thrownError);
        },
   	 timeout: 60000,
     crossDomain: true,
	});
};

function showTestResults(results) {
  window.testResults = results;

  const $testRuns = document.getElementById("testRuns");
  const $testFailures = document.getElementById("testFailures");
  const $progressSuccess = document.getElementById("testResultProgressSuccess");
  const $progressFailures = document.getElementById("testResultProgressFailures");

  const successfulTests = results.filter(r => r.failedVerifications.length == 0);
  const failedTests = results.filter(r => r.failedVerifications.length > 0);

  const runs = results.length;
  const failures = failedTests.length;

  $testRuns.innerHTML = runs;
  $testFailures.innerHTML = failures;

  $progressSuccess.style.width = 100 * (1 - (failures / runs)) + "%";
  $progressFailures.style.width = 100 * (failures / runs) + "%";

  document.getElementById("testCases").childNodes.forEach(c => {
      const testName = c.testName;
      const $status = c.firstChild;

      if (successfulTests.filter(t => t.name == testName).length == 1) {
        $status.innerHTML = "✓";
        $status.classList.add("successful-test");
      } else if (failedTests.filter(t => t.name == testName).length == 1) {
        $status.innerHTML = "✗";
        $status.classList.add("failed-test");
      }
  });

  renderTestCase(currentTest);

  // --------- token party
  if (window.tokenParty == true) {
    elementRegistry.forEach(element => {
      tokenCount.removeTokenCount(element);
    });

    if (failures == 0) {
        elementRegistry.forEach(element => {
          if (!element.waypoint && Math.random() > 0.25) {
            const symbols = ["☕", "✨", "✆", "✋", "❗", "➠", "☂", "♨", "♻", "⚛"]
            tokenCount.addTokenCount(element, symbols[Math.round(Math.random() * (symbols.length - 1))]); // ✨
          }
        });
    }
  }
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
window.testResults = testResults;
