import React from 'react'
import './App.css'
import { NODE_ID } from "./services/urls"
import Network from "./components/Network"
import Header from "./components/Header"
import Flows from "./components/Flows"
import Cordapps from "./components/Cordapps";
import { useEffect } from 'react';


function App() {

    useEffect(() => {
        document.title = `${NODE_ID}'s Application`
    });

  return (
    <div className="vh-100 avenir bg-dark-gray">
        <Header/>
        <div className="flex flex">
            <div className="w-30">
                <Network/>
            </div>
            <div className="w-30">
                <Flows/>
            </div>
            <div className="w-30">
                <Cordapps/>
            </div>
        </div>
    </div>
  );
}
export default App;
