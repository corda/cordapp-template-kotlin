import React from 'react';
import ReactDOM from 'react-dom';
// import { Button, FormControl, Grid, InputLabel, MenuItem, Select, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow, TextField, FormHelperText } from '@material-ui/core';

const trimFlowsForDisplay = (text) => {
    var words = text.split(".")
    console.log("words" + words)
    return words[words.length - 1]
}

const Modal = ({ flow, isShowing, hide }) => isShowing ? ReactDOM.createPortal(

    <React.Fragment>
        <div className="modal-overlay avenir"/>
        <div className="modal-wrapper" aria-modal aria-hidden tabIndex={-1} role="dialog">
            <div className="modal">
                <div className="modal-header">
                    <button type="button" className="modal-close-button bg-transparent" data-dismiss="modal" aria-label="Close" onClick={hide}>
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div style={{color: "red"}}>{trimFlowsForDisplay(flow)}</div>
            </div>
        </div>
    </React.Fragment>, document.body
) : null;

export default Modal;