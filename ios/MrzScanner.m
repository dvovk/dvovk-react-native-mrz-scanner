#import "MrzScanner.h"

@implementation MrzScanner

- (instancetype)init
{
    self = [super init];
    if (self) {
        
        check_digits = [NSArray arrayWithObjects:@"A", @"B", @"C", @"D", @"E", @"F", @"G", @"H", @"I", @"J", @"K", @"L", @"M", @"N", @"O", @"P", @"Q", @"R", @"S", @"T", @"U", @"V", @"W", @"X", @"Y", @"Z", nil];
        FIRVision *vision = [FIRVision vision];
        textRecognizer = [vision onDeviceTextRecognizer];

        metadata = [[FIRVisionImageMetadata alloc] init];
        AVCaptureDevicePosition cameraPosition =
            AVCaptureDevicePositionBack;  // Set to the capture device you used.
        metadata.orientation =
            [self imageOrientationFromDeviceOrientation:UIDevice.currentDevice.orientation
                                         cameraPosition:cameraPosition];
        
        [self checkForCameraPermission];
    }
    return self;
}

-(void)checkForCameraPermission
{
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if(status == AVAuthorizationStatusAuthorized)
        [self showCamera];
    else if(status == AVAuthorizationStatusDenied)
    {
        [self permissionDenied];
    }
    else
    {
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL garanted){
            if(garanted)
               [self showCamera];
            else
            {
                [self permissionDenied];
            }
        }];
    }
}

-(void)permissionDenied
{
    if(!self.onPermissionResult)
    {
        return;
    }
    
    self.onPermissionResult(@{
       @"result":@"denied"
     });
}

-(void)scanSuccess
{
    int check_digit = [self getCheckDigitForText:pnum];
    ios_mrz_check = [NSString stringWithFormat:@"%@%d", pnum, check_digit];
    check_digit = [self getCheckDigitForText:pdob];
    ios_mrz_check = [ios_mrz_check stringByAppendingString:[NSString stringWithFormat:@"%@%d", pdob, check_digit]];
    check_digit = [self getCheckDigitForText:pexp];
    ios_mrz_check = [ios_mrz_check stringByAppendingString:[NSString stringWithFormat:@"%@%d", pexp, check_digit]];
    
    if(!self.onScanSuccess)
    {
        return;
    }
    
    self.onScanSuccess(@{
       @"data": @{
         @"num": pnum,
         @"dob": pdob,
         @"exp": pexp,
         @"ios_mrz_check": ios_mrz_check
       }
     });
}

-(void)showCamera
{
    captureSession = [[AVCaptureSession alloc] init];
    captureSession.sessionPreset = AVCaptureSessionPreset640x480;
    
    AVCaptureDevice *backCamera = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    AVCaptureDeviceInput *input = [[AVCaptureDeviceInput alloc] initWithDevice:backCamera error:nil];
    
    stillImageOutput = [[AVCaptureVideoDataOutput alloc] init];
    [stillImageOutput setSampleBufferDelegate:self queue:dispatch_get_main_queue()];
    
    if([captureSession canAddInput:input] && [captureSession canAddOutput:stillImageOutput])
    {
        [captureSession addInput:input];
        [captureSession addOutput:stillImageOutput];
        [self setupLivePreview];
    }
}

-(void)setupLivePreview
{
    videoPreviewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:captureSession];
    videoPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    videoPreviewLayer.connection.videoOrientation = [self getOrientation];
    
    [captureSession startRunning];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        self->videoPreviewLayer.frame = self.bounds;
        [[self layer] addSublayer:self->videoPreviewLayer];
    });
}

-(void)recognizeText:(UIImage *)img
{
    FIRVisionImage *image = [[FIRVisionImage alloc] initWithImage:img];
    image.metadata = metadata;
    
    [textRecognizer processImage:image completion:^(FIRVisionText *_Nullable result, NSError *_Nullable error) {
        if (error != nil || result == nil)
            return;

        NSLog(result.text);
        [self parseData:result.blocks];
    }];
}

-(void)crop:(CMSampleBufferRef) sampleBuffer
{
    CVPixelBufferRef imageBuffer1 = CMSampleBufferGetImageBuffer(sampleBuffer);
    
    CIImage *ciimage = [[CIImage alloc] initWithCVPixelBuffer:imageBuffer1];
    ciimage = [ciimage imageByCroppingToRect:CGRectMake(/*offset from bottom*/240, 0, 120, 640)];
    UIImage *image = [self convert:ciimage];
    
    [self recognizeText:image];
}

-(UIImage *)convert:(CIImage *)cmage
{
    CIContext *context = [[CIContext alloc] initWithOptions:nil];
    CGImageRef cgImage = [context createCGImage:cmage fromRect:cmage.extent];
    UIImage *image = [[UIImage alloc] initWithCGImage:cgImage];
    return image;
}

-(void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(nonnull CMSampleBufferRef)sampleBuffer fromConnection:(nonnull AVCaptureConnection *)connection
{
    if(!is_success)
    {
        [self crop:sampleBuffer];
    }
}

-(void)parseData:(NSArray *)items
{
    if([items count] > 1)
    {
        for(int i = 0; i < [items count]; i++)
        {
            FIRVisionTextBlock *item = [items objectAtIndex:i];
            
            if([self checkMRZFirstLine:item.text])
            {
                int li = i + 1;
                if(li < [items count])
                {
                    if([self grabPassportData:((FIRVisionTextBlock *)[items objectAtIndex:li]).text])
                    {
                        if(!is_success)
                        {
                            [captureSession stopRunning];
                            is_success = true;
                            [self scanSuccess];
                        }
                        
                    }
                }
            }
        }
    }
}

-(BOOL)checkMRZFirstLine:(NSString *)val
{
    NSString *trimed = [val stringByReplacingOccurrencesOfString:@" " withString:@""];

    if([trimed length] > 1)
    {
        NSString *p_start = [trimed substringToIndex:2];
        if ([p_start isEqualToString:@"P<"])
            return true;
        else
            return false;
    }
    else
        return false;
}

-(BOOL)matches:(NSString *)reg inText:(NSString *)text
{
    NSError* error = nil;
    NSRegularExpression* regex = [NSRegularExpression regularExpressionWithPattern:reg options:0 error:&error];
    
    NSArray* matches = [regex matchesInString:text options:0 range:NSMakeRange(0, [text length])];
      
    return ([matches count] == [text length]);
    
    //return([regex matchesInString:text options:0 range:NSMakeRange(0, [text length])] != nil);
    
}

-(BOOL)isDateValid:(NSString *)text
{
    BOOL match = [self matches:@"[0-9]" inText:text];
    return match;
}

-(int)getCheckDigitForText:(NSString *)text
{
    int sum = 0;

    for(int i = 0; i < [text length]; i++)
    {
        NSString *s = [text substringWithRange:NSMakeRange(i, 1)];
        int dg = [self getDigitForValue:s withWeight:i];
        sum += dg;
    }

    int check_digit = sum % 10;
    return check_digit;
}

-(BOOL)isStringValid:(NSString *)text check:(NSString *)check
{
    if(![self isDateValid:check])
        return false;

    int check_digit = [self getCheckDigitForText:text];
    int str_check = [check intValue];
    
    if(check_digit == str_check)
        return true;

    return false;
}

-(int)getMultiplier:(int)weight
{
    int cw = weight % 3;

    if(cw == 0)
        return 7;
    else if(cw == 1)
        return 3;
    else
        return 1;
}

-(int)getDigitForValue:(NSString *)val withWeight:(int)weight
{
    int mult = [self getMultiplier:weight];

    int res = 0;
    if([val isEqualToString:@"<"])
    {
        return res;
    }
    else if([self isDateValid:val])
    {
        res = [val intValue];
        return res * mult;
    }
    else
    {
        for(int i = 0; i < [check_digits count]; i++)
        {
            if([val isEqualToString:[check_digits objectAtIndex:i]])
            {
                return (i + 10) * mult;
            }
        }
    }

    return res;
}

-(BOOL)grabPassportData:(NSString *)val
{
    NSString *trimed = [val stringByReplacingOccurrencesOfString:@" " withString:@""];

    if([trimed length] == 44)
    {
        pnum = [trimed substringToIndex:9];
        NSString *passNumberCheckDigit = [trimed substringWithRange:NSMakeRange(9, 1)];
        
        if(![self isStringValid:pnum check:passNumberCheckDigit])
            return false;
        
        
        pdob = [trimed substringWithRange:NSMakeRange(13, 6)];
        pexp = [trimed substringWithRange:NSMakeRange(21, 6)];
        
        if(![self isDateValid:pdob] || ![self isDateValid:pexp])
            return false;
        
        NSString *a = [trimed substringWithRange:NSMakeRange(0, 10)];
        NSString *b = [trimed substringWithRange:NSMakeRange(13, 7)];
        NSString *c = [trimed substringWithRange:NSMakeRange(21, 22)];
        NSString *d = [trimed substringWithRange:NSMakeRange(43, 1)];
        NSString *st = [NSString stringWithFormat:@"%@%@%@", a,b,c];
        
        

        if(![self isStringValid:st check:d])
            return false;
        else
        {
            return true;
        }
    }
    else
    {
        return false;
    }
}

-(AVCaptureVideoOrientation)getOrientation
{
    return AVCaptureVideoOrientationPortrait;
    /*
    UIDeviceOrientation deviceOrientation = [[UIDevice currentDevice] orientation];
    
    if(deviceOrientation == UIDeviceOrientationPortrait)
        return AVCaptureVideoOrientationPortrait;
    else if(deviceOrientation == UIDeviceOrientationPortraitUpsideDown)
        return AVCaptureVideoOrientationPortraitUpsideDown;
    else if(deviceOrientation == UIDeviceOrientationLandscapeLeft)
        return AVCaptureVideoOrientationLandscapeLeft;
    else
        return AVCaptureVideoOrientationLandscapeRight;*/
}

- (FIRVisionDetectorImageOrientation)
    imageOrientationFromDeviceOrientation:(UIDeviceOrientation)deviceOrientation
                           cameraPosition:(AVCaptureDevicePosition)cameraPosition {
    
    return FIRVisionDetectorImageOrientationRightTop;
 /* switch (deviceOrientation) {
    case UIDeviceOrientationPortrait:
      if (cameraPosition == AVCaptureDevicePositionFront) {
        return FIRVisionDetectorImageOrientationLeftTop;
      } else {
        return FIRVisionDetectorImageOrientationRightTop;
      }
    case UIDeviceOrientationLandscapeLeft:
      if (cameraPosition == AVCaptureDevicePositionFront) {
        return FIRVisionDetectorImageOrientationBottomLeft;
      } else {
        return FIRVisionDetectorImageOrientationTopLeft;
      }
    case UIDeviceOrientationPortraitUpsideDown:
      if (cameraPosition == AVCaptureDevicePositionFront) {
        return FIRVisionDetectorImageOrientationRightBottom;
      } else {
        return FIRVisionDetectorImageOrientationLeftBottom;
      }
    case UIDeviceOrientationLandscapeRight:
      if (cameraPosition == AVCaptureDevicePositionFront) {
        return FIRVisionDetectorImageOrientationTopRight;
      } else {
        return FIRVisionDetectorImageOrientationBottomRight;
      }
    default:
      return FIRVisionDetectorImageOrientationTopLeft;
  }*/
}

@end
