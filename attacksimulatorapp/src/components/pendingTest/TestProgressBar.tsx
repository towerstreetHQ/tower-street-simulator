import * as React from 'react';
import ProgressbarBackground from '../../img/bg-test-progressbar.svg';

const TestProgressBar = ({ value }: { value: number }) => {
  return (
    <div className="test-progressbar">
      <svg className="progressbar" viewBox="0 0 400 310" version="1.1">
        <defs>
          <linearGradient id="linear" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#1A80E8" />
            <stop offset="50%" stop-color="#1A80E8" />
            <stop offset="100%" stop-color="#4FCEFA" />
          </linearGradient>
        </defs>
        <g id="Page-1" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
          <g className="progress-line" stroke="url(#linear)" strokeWidth="3.5">
            <polyline points="-4.54747351e-13 243.62 56.743728 243.62 66.915545 309.459592 66.915545 243.62 133.375545 177.3 133.375545 66.4 199.835545 0.9 332.735545 131.9 332.735545 243.62 333.275545 297.15 343.768669 243.62 399.46 243.62" />
          </g>
        </g>
      </svg>
      <img className="progressbar-bg" src={ProgressbarBackground} />
      <div className="stat">
        <strong>{`${Math.floor(value * 100)}%`}</strong><br />
        completed
      </div>
    </div>
  );
};

export default TestProgressBar;
