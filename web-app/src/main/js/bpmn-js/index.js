module.exports = {
  __init__ : [ "animation", "tokenCount" ],
  animation : [ "type", require("./Animation").default ],
  tokenCount: [ "type", require("./TokenCount").default ]
}
