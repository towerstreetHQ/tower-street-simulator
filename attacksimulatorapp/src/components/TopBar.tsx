import * as React from 'react';
import ASlogo from '../img/logo-attack-simulator.svg';
import TSlogo from '../img/logo-towerstreet.svg';

const TopBar = () => {
  return (
    <>
      <div className="top-bar-container" data-sticky-container>
        <div id="top-bar" className="top-bar" data-topbar>
          <div className="top-bar-left">
            <a href="/" title="Tower Street Cyber Health Check Homepage">
              <img
                src={TSlogo}
                alt="Tower Street"
                className="top-bar__logo"
              />
            </a>
            <img
              src={ASlogo}
              alt="Tower Street"
              className="show-for-scanner"
            />
          </div>
        </div>
      </div>
    </>
  );
};

export default TopBar;
