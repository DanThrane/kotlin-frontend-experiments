{
  mode: 'development',
  resolve: {
    modules: [
      'node_modules'
    ]
  },
  plugins: [],
  module: {
    rules: [
      {
        test: /\.js$/,
        use: [
          'source-map-loader'
        ],
        enforce: 'pre'
      },
      {
        test: /\.js$/,
        use: [
          'source-map-loader'
        ],
        enforce: 'pre'
      }
    ]
  },
  entry: [
    '/home/dthrane/projects/kotlin-frontend-experiments/build/js/packages/web2/kotlin/web2.js',
    'source-map-support/browser-source-map-support.js'
  ],
  output: {
    path: '/home/dthrane/projects/kotlin-frontend-experiments/build/libs',
    filename: 'web2.js'
  },
  devtool: 'eval-source-map',
  devServer: {
    inline: true,
    lazy: false,
    noInfo: true,
    open: true,
    overlay: false,
    port: 8080,
    contentBase: [
      '/home/dthrane/projects/kotlin-frontend-experiments/build/processedResources/Js/main'
    ]
  }
}