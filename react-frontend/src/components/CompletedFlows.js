import React, {useEffect} from 'react';

import '../styling/CompletedFlows.css'

function CompletedFlows({flows, setRefresh}) {
    function clearHistory() {
        localStorage.clear()
        setRefresh(true)
    }

    return (
        <div>
            <div className="table-wrapper">
                <h1 style={{marginLeft: 65}}>Flow History</h1>
                <div className="tbl-header">
                    <table cellPadding="0" cellSpacing="0" border="0">
                        <thead>
                        <tr>
                            <td>Flow Name</td>
                            <td>Status</td>
                        </tr>
                        </thead>
                    </table>
                </div>
                <div className="tbl-content">
                    <table cellPadding="0" cellSpacing="0" border="0" className="completed-flows">
                        {flows.splice(0).reverse().map((flow, index) => {
                            const { flowName, flowCompletionStatus} = flow
                            return (
                                <tr key={index}>
                                    <td>{flowName}</td>
                                    <td style={{color: flowCompletionStatus? 'green': 'red'}}>{flowCompletionStatus ? "Success" : "Failed"}</td>
                                </tr>
                            )
                        })
                        }
                    </table>
                </div>
            </div>
            <h1>
                <a type="button"
                   className="btn btn-2"
                   onClick={clearHistory}>Clear History</a>
            </h1>
        </div>
    );
}
export default CompletedFlows;