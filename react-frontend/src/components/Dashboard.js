import React from 'react';
import NetworkParticipants from "./NetworkParticipants";
import NetworkInfo from "./NetworkInfo";
import Cordapps from "./Cordapps";
import { makeStyles } from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
        paddingLeft: "200px"
    }
}));

function Dashboard() {
    const classes = useStyles();

    return (
        <div className={classes.root}>
            <Grid container  direction="row"
                  justify="center"
                  alignItems="stretch"
                  spacing={3}>
                <Grid item xs>
                    <NetworkParticipants/>
                </Grid>
                {/*<Grid item xs>*/}
                {/*    <NetworkInfo/>*/}
                {/*</Grid>*/}
                <Grid item xs>
                    <Cordapps/>
                </Grid>
            </Grid>
        </div>
    );
}
export default Dashboard;
