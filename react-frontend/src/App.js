import React from 'react'
import './App.css'
import { NODE_ID } from "./services/urls"
import Header from "./components/Header"
import Dashboard from "./components/Dashboard";
import Network from "./components/Network"
import Flows from "./components/Flows"
import Vault from "./components/Vault";
import Cordapps from "./components/Cordapps";
import { useEffect, useState } from 'react';
import Sidebar from "./components/Sidebar";


function App() {
    const [currentPage, setCurrentPage] = useState(0)

    useEffect(() => {
        document.title = `${NODE_ID}'s Application`
    });

  return (
    // <div className="vh-100 avenir">
    <div className="vh-100 avenir bg-dark-gray">
        <Header/>
        <Sidebar currentPage={currentPage} setCurrentPage={setCurrentPage} />
        <div style={{marginLeft: 120, paddingTop: 70}}>
            <div className="content-pane">
                {/*{*/}
                {/*    this.props.spinner?*/}
                {/*        <div className="spinner">*/}
                {/*            <div>*/}
                {/*                <img style={{width: 100}} src="spinner.svg" alt="Spinner"></img>*/}
                {/*            </div>*/}
                {/*        </div>:null*/}
                {/*}*/}
                {
                    currentPage === 0 ? <Dashboard/>:
                    currentPage === 1 ? <Flows/>:
                    currentPage === 2 ? <Vault/>:
                    <Dashboard/>
                }
            </div>
        </div>
        {/*<div className="flex flex">*/}
        {/*    <div className="w-30">*/}
        {/*        <Network/>*/}
        {/*    </div>*/}
        {/*    <div className="w-30">*/}
        {/*        <Flows/>*/}
        {/*    </div>*/}
        {/*    <div className="w-30">*/}
        {/*        <Cordapps/>*/}
        {/*    </div>*/}
        {/*</div>*/}
    </div>
  );
}
export default App;
