#import "MrzScannerManager.h"
#import "MrzScanner.h"

@implementation MrzScannerManager

RCT_EXPORT_MODULE()

RCT_EXPORT_VIEW_PROPERTY(onPermissionResult, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onScanSuccess, RCTBubblingEventBlock)

- (UIView *)view
{
  return [[MrzScanner alloc] init];
}

@end
