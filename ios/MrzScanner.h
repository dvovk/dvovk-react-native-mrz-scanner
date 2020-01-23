#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>
#import <React/RCTViewManager.h>

#import <Firebase/Firebase.h>
//#import <Firebase/Firebase.h>

@interface MrzScanner : UIView <AVCaptureVideoDataOutputSampleBufferDelegate>
{
    AVCaptureSession *captureSession;
    AVCaptureVideoDataOutput *stillImageOutput;
    AVCaptureVideoPreviewLayer *videoPreviewLayer;
    FIRVisionTextRecognizer *textRecognizer;
    FIRVisionImageMetadata *metadata;
    BOOL is_success;
    NSArray *check_digits;
    NSString *pnum;
    NSString *pdob;
    NSString *pexp;
    NSString *ios_mrz_check;
}

@property (nonatomic, copy) RCTBubblingEventBlock onPermissionResult;
@property (nonatomic, copy) RCTBubblingEventBlock onScanSuccess;

@end
