import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';


const useStyles = makeStyles({
    table: {
        minWidth: 650,
    },
});
function CompletedFlows({flows}) {
    const classes = useStyles();

    return (
        // <TableContainer component={Paper}>
        <div>
        <table>
            {flows.map((flow, index) => {
                const { flowName, flowCompletionStatus} = flow //destructuring
                return (
                    <tr key={index}>
                        <td>{index}</td>
                        <td>{flowName}</td>
                        <td>{flowCompletionStatus ? "Success" : "Failed"}</td>
                    </tr>
                )
            })
            }
        </table>
        </div>
        //     }
        //     <Table className={classes.table} aria-label="simple table">
        //         <TableHead>
        //             <TableRow>
        //                 <TableCell>Dessert (100g serving)</TableCell>
        //                 <TableCell align="right">Calories</TableCell>
        //                 <TableCell align="right">Fat&nbsp;(g)</TableCell>
        //                 <TableCell align="right">Carbs&nbsp;(g)</TableCell>
        //                 <TableCell align="right">Protein&nbsp;(g)</TableCell>
        //             </TableRow>
        //         </TableHead>
        //         <TableBody>
        //             {rows.map((row) => (
        //                 <TableRow key={row.name}>
        //                     <TableCell component="th" scope="row">
        //                         {row.name}
        //                     </TableCell>
        //                     <TableCell align="right">{row.calories}</TableCell>
        //                     <TableCell align="right">{row.fat}</TableCell>
        //                     <TableCell align="right">{row.carbs}</TableCell>
        //                     <TableCell align="right">{row.protein}</TableCell>
        //                 </TableRow>
        //             ))}
        //         </TableBody>
        //     </Table>
        // </TableContainer>
    );
}
export default CompletedFlows;