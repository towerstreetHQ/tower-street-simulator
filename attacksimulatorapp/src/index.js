import React from 'react';
import ReactDOM from 'react-dom';
import 'url-polyfill/url-polyfill';
import App from './App';
import './scss/app.scss';
import { config } from './lib/dotenv';
import registerServiceWorker from './registerServiceWorker';

// String.endsWith polyfill
if (!String.prototype.endsWith) {
    // eslint-disable-next-line no-extend-native
    String.prototype.endsWith = function(search, this_len) {
        if (this_len === undefined || this_len > this.length) {
            this_len = this.length;
        }
        return this.substring(this_len - search.length, this_len) === search;
    };
}

// String.startsWith polyfill
if (!String.prototype.startsWith) {
    // eslint-disable-next-line no-extend-native
	String.prototype.startsWith = function(search, pos) {
		return this.substr(!pos || pos < 0 ? 0 : +pos, search.length) === search;
	};
}

// String includes polyfill
if (!String.prototype.includes) {
    // eslint-disable-next-line no-extend-native
    Object.defineProperty(String.prototype, 'includes', {
      value: function(search, start) {
        if (typeof start !== 'number') {
          start = 0
        }
        
        if (start + search.length > this.length) {
          return false
        } else {
          return this.indexOf(search, start) !== -1
        }
      }
    })
  }

config();
ReactDOM.render(<App />, document.getElementById('root'));
registerServiceWorker();
