import * as React from 'react';
import TopBar from './TopBar';

const Layout = ({
  children,
  lightVariant,
}: {
  children: React.ReactNode | React.ReactNode[];
  lightVariant?: boolean;
}) => (
  <div className={`main ${lightVariant ? 'layout-light' : ''}`}>
    <TopBar />
    {children}
  </div>
);

export default Layout;
