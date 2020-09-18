import React from 'react';
import '../styling/CompletedFlows.css'

function CompletedFlows({flows}) {

    return (
    <div className="table-wrapper">
        <h1>
        <a type="button"
           className="btn btn-3 center"
           onClick={localStorage.clear}>Flow History</a>
        </h1>
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
            {flows.map((flow, index) => {
                const { flowName, flowCompletionStatus} = flow
                return (
                    <tr key={index}>
                        <td>{flowName}</td>
                        <td style={{color: flowCompletionStatus? 'green': 'red' }}>{flowCompletionStatus ? "Success" : "Failed"}</td>
                    </tr>
            // {jsonToArray(flows).map((flow, index) => {
            //
            //     return (
            //         <tr key={index}>
            //             <td>{flow.value.flowName}</td>
            //             <td style={{color: flow.value.flowCompletionStatus? 'green': 'red' }}>{flow.value.flowCompletionStatus ? "Success" : "Failed"}</td>
            //         </tr>
                )
            })
            }
        </table>
        </div>
    </div>
    );
}
export default CompletedFlows;