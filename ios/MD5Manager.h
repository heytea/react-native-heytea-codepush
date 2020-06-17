//
//  MD5Manager.h
//  heyteago
//
//  Created by Chris Zhou on 2020/6/15.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CommonCrypto/CommonDigest.h>

NS_ASSUME_NONNULL_BEGIN

@interface MD5Manager : NSObject

+(NSString *)md5:(NSString *)input;

@end

NS_ASSUME_NONNULL_END
