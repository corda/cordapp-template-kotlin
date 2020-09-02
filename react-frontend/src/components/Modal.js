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
        <div className="modal-overlay"/>
        <div className="modal-wrapper" aria-modal aria-hidden tabIndex={-1} role="dialog">
            <div className="modal">
                {/*<div className="modal-header">*/}
                    <button type="button" className="modal-close-button" data-dismiss="modal" aria-label="Close" onClick={hide}>
                        <span aria-hidden="true">&times;</span>
                    </button>
                {/*</div>*/}
                <div style={{color: "red"}}>{trimFlowsForDisplay(flow)}</div>
                {/*{*/}
                {/*    this.state.selectedFlow.constructors && Object.keys(this.state.selectedFlow.constructors).length>0?*/}
                {/*        <div style={{width: "30%", float: "left"}}>*/}
                {/*            <FormControl style={{width:"100%"}}>*/}
                {/*                <div style={{paddingLeft: 10}}>*/}
                {/*                    <InputLabel id="flow-cons-select-label" style={{paddingLeft: 10}}>Select A Constructor Type</InputLabel>*/}
                {/*                    <Select labelId="flow-cons-select-label" onChange={this.handleFlowConstructorSelection}*/}
                {/*                            value={this.state.selectedFlow.activeConstructor} fullWidth>*/}
                {/*                        {*/}
                {/*                            Object.keys(this.state.selectedFlow.constructors).map((constructor, index) => {*/}
                {/*                                return(*/}
                {/*                                    <MenuItem key={index} value={constructor}>{constructor}</MenuItem>*/}
                {/*                                );*/}
                {/*                            })*/}
                {/*                        }*/}
                {/*                    </Select>*/}
                {/*                </div>*/}
                {/*            </FormControl>*/}
                {/*        </div>:null*/}
                {/*}*/}
            </div>
        </div>
    </React.Fragment>, document.body
) : null;

export default Modal;