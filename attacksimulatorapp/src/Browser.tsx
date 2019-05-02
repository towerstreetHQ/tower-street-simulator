// Contains helpers for checking browsers using bowser library
// See: https://github.com/lancedikson/bowser

// Bundled version contains polyfils for all browsers. We faced issue that find method
// is not supported in IE11 and whole application crashed.
// https://github.com/lancedikson/bowser/issues/264
// open PR for types https://github.com/lancedikson/bowser/pull/289
// @ts-ignore
import { getParser } from 'bowser/bundled';

// One time initialized parser out of UA string
const parser = getParser(window.navigator.userAgent);
const parsed = parser.parse();
const campaign = process.env.REACT_APP_SIMULATOR_CAMPAIGN_MODE;

// User browser will be compared to following settings
// Also these values will be printed out if wrong browser is detected
const commonBrowsers = [
  { id: 'chromium', name: 'Chromium', version: '57', logo: 'Chromium_Logo.svg' },
  { id: 'chrome', name: 'Google Chrome', version: '57', logo: 'Google_Chrome_Logo.svg' },
  { id: 'firefox', name: 'Mozilla Firefox', version: '54', logo: 'Firefox_Logo.svg' },
];

const desktopBrowsers = [
  // Edge use different version number in user agent than real edge versions
  // Number in UA is derived from version of windows
  {
    id: 'Microsoft Edge',
    name: 'Microsoft Edge',
    version: '42',
    detectVersion: '17',
    logo: 'Microsoft_Edge_Logo.svg',
  },
];

const mobileBrowsers = [
  {
    id: 'Microsoft Edge',
    name: 'Microsoft Edge (mobile)',
    version: '14',
    logo: 'Microsoft_Edge_Logo.svg',
  },
];

const campaignSpecificBrowsers = [
  { id: 'safari', name: 'Safari', version: '9', logo: 'Safari_Logo.svg' },
];

interface BrowserItem {
  [key: string]: string;
}

interface Browser {
  id: string;
  name: string;
  version: string;
  detectVersion?: string;
  logo: string;
}

export const supportedBrowsers = campaign
  ? [...commonBrowsers, ...desktopBrowsers, ...mobileBrowsers, ...campaignSpecificBrowsers]
  : [...commonBrowsers, ...desktopBrowsers, ...mobileBrowsers];

export const parseBrowserList = (browserList: Browser[]) => {
  // Create browser detection object for bowser lib
  //  For syntax see: https://github.com/lancedikson/bowser#filtering-browsers
  return browserList.reduce((browsersAcc: BrowserItem, browser) => {
    const { detectVersion, version, id } = browser;
    browsersAcc[id] = '>' + (detectVersion || version);
    return browsersAcc;
  }, {});
};

export const checkBrowser = () => {
  const common = parseBrowserList([...commonBrowsers, ...campaignSpecificBrowsers]);
  const desktop = parseBrowserList(desktopBrowsers);
  const mobile = parseBrowserList(mobileBrowsers);
  // platform based checking browser
  const isValidBrowser = {
    desktop: {
      ...common,
      ...desktop,
    },
    mobile: {
      ...common,
      ...mobile,
    },
    tablet: { ...common, ...desktop },
  };
  return parser.satisfies(isValidBrowser);
};

export function getBrowserData() {
  return parsed ? parsed.parsedResult : undefined;
}
