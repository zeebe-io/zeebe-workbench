var path = require('path');

module.exports = {
  mode: 'development',
  entry: './src/main/js/app.js',
  output: {
    path: path.resolve(__dirname, 'src/main/webapp/js'),
    filename: 'app.bundled.js'
  },
  module: {
    rules: [
      {
        test: /\.bpmn$/,
        use: {
          loader: 'raw-loader'
        }
      }
    ]
  }
};