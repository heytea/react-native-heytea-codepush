//
//  MD5Manager.m
//  heyteago
//
//  Created by Chris Zhou on 2020/6/15.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "MD5Manager.h"

@implementation MD5Manager

+(NSString *)md5:(NSString *)input{
  const char *cStr = [input UTF8String];
  unsigned char digest[CC_MD5_DIGEST_LENGTH];
  CC_MD5(cStr, (CC_LONG)strlen(cStr), digest);
  NSMutableString *outputStr = [NSMutableString stringWithCapacity:CC_MD5_DIGEST_LENGTH *2];
  for (int i = 0; i < CC_MD5_DIGEST_LENGTH ; i++) {
    [outputStr appendFormat:@"%02x",digest[i]];
  }
  return outputStr;
}


@end
