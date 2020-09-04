package uk.departure.dashboard;

interface IEngineCallback {

    //Receives engine current output between 0 (min) an 1 (100%)
    oneway void outputPower(float value);

}
