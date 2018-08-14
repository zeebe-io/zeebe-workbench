'use strict';

import { domify } from 'min-dom';

var OFFSET_BOTTOM = 10,
    OFFSET_LEFT = -15;

var LOW_PRIORITY = 500;

function TokenCount(eventBus, overlays, elementRegistry, canvas) {
  var self = this;

  this._overlays = overlays;
  this._elementRegistry = elementRegistry;
  this._canvas = canvas;

  this.overlayIds = {};
}

TokenCount.prototype.addTokenCount = function(element, tokenCount) {
  var html = this.createTokenCount(tokenCount);

  var position = { bottom: OFFSET_BOTTOM, left: OFFSET_LEFT };

  var overlayId = this._overlays.add(element, 'token-count', {
    position: position,
    html: html,
    show: {
      minZoom: 0.5
    }
  });

  this.overlayIds[element.id] = overlayId;
};

TokenCount.prototype.createTokenCount = function(tokenCount) {
  return domify('<div class="token-count waiting">' + tokenCount + '</div>');
};

TokenCount.prototype.removeTokenCount = function(element) {
  var overlayId = this.overlayIds[element.id];

  if (!overlayId) {
    return;
  }

  this._overlays.remove(overlayId);

  delete this.overlayIds[element.id];
};

TokenCount.$inject = [ 'eventBus', 'overlays', 'elementRegistry', 'canvas' ];

export default TokenCount;
