import React from "react";
import {FormControl, InputLabel, MenuItem, Select} from "@material-ui/core";

function ConstuctorSelection() {
    const [activeConstructor, setActiveConstructor] = React.useState([])
    const [constructors, setConstructors] = React.useState([])

    function loadConstructors() {
        setConstructors([])
    }

    function handleFlowConstructorSelection(event) {
        setActiveConstructor([event.target.value])
    }


    return (
        <div>
            {  Object.keys(constructors) > 0 ?
                <div style={{width: "30%", float: "left"}}>
                    <FormControl style={{width:"100%"}}>
                        <div style={{paddingLeft: 10}}>
                            <InputLabel id="flow-cons-select-label" style={{paddingLeft: 10}}>Select A Constructor Type</InputLabel>
                            <Select labelId="flow-cons-select-label" onChange={event => handleFlowConstructorSelection(event)}
                                    value={activeConstructor} fullWidth>
                                {
                                    Object.keys(constructors).map((constructor, index) => {
                                        return(
                                            <MenuItem key={index} value={constructor}>{constructor}</MenuItem>
                                        );
                                    })
                                }
                            </Select>
                        </div>
                    </FormControl>
                </div>: null
            }
        </div>
    )
}

export default ConstuctorSelection