import PropTypes from 'prop-types';
import React from 'react';
import {requireNativeComponent} from 'react-native';

class MrzScanner extends React.Component {
    constructor(props) {
        super(props);
        this._onScanSuccess = this._onScanSuccess.bind(this);
    }

    _onPermissionResult = (event) => {
    if (!this.props.onPermissionResult) {
      return;
    }

    // process raw event...
    this.props.onPermissionResult(event.nativeEvent);
  }

  _onScanSuccess = (event) => {
    if (!this.props.onScanSuccess) {
      return;
    }

    // process raw event...
    this.props.onScanSuccess(event.nativeEvent);
  }

  render() {
    return <MRZ 
        {...this.props}
        //onPermissionResult={this._onPermissionResult}
        onScanSuccess={this._onScanSuccess}
     />;
  }
}

MrzScanner.propTypes = {
  /**
   * A Boolean value that determines whether the user may use pinch
   * gestures to zoom in and out of the map.
   */
  onPermissionResult: PropTypes.func,
  onScanSuccess: PropTypes.func,
};

var MRZ = requireNativeComponent('MrzScanner', MrzScanner);

module.exports = MrzScanner;
