import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';

//.Cordapps {
//     overflow: scroll;
//     height: auto;
//     max-height: 400px;
//     margin: 20px;
//     border-top: 1px solid white;
//     border-bottom: 1px solid white;
//     font-size: 20px;
//     width: 75%;
// }
//
// .cordapp-wrapper{
//     width: 100%;
//     display: inline-block;
// }
//
// .appInfo{
//     padding: 10px;
//     color: #CCCCCC;
// }

const useStyles = makeStyles({
    table: {
        minWidth: 650,
        height: "auto",
        maxHeight: 400,
        width: "75%",
        color: "white",
        backgroundColor: "#333333",


    },
});
function CompletedFlows({flows}) {

    const classes = useStyles();


    function getFlows() {
        console.log("FLOWS ARE: " + flows[0].flowName)
        const myJSON = JSON.parse(flows);
        localStorage.setItem("flows", myJSON)
        console.log("MYJSON: " + myJSON)
        const localFlows = localStorage.getItem("flows")
        console.log("localFlows: " + localFlows[0])

        return localFlows ? flows : null
    }

    return (
        <TableContainer>
             <Table className={classes.table} aria-label="simple table">
                 <TableHead>
                     <TableRow>
                         <TableCell>Id</TableCell>
                         <TableCell align="right">Flow Name</TableCell>
                         <TableCell align="right">Status</TableCell>
                     </TableRow>
                 </TableHead>
                 <TableBody>
                     {flows.map((flow, index) => {
                         const { flowName, flowCompletionStatus} = flow
                         return (
                            <TableRow key={index}>
                                <TableCell component="th" scope="row">
                                    {index}
                                </TableCell>
                                <TableCell align="right">{flowName}</TableCell>
                                <TableCell align="right">{flowCompletionStatus ? "Success" : "Failed"}</TableCell>
                            </TableRow>
                         )
                        })
                     }
                 </TableBody>
             </Table>
         </TableContainer>
    );
}
export default CompletedFlows;