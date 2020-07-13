//
//  RCTUdesk.m
//  heyteago
//
//  Created by Chris Zhou on 2020/6/11.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "RNHeyteaCodePush.h"
#import "RNHeyteaDownloader.h"
#import <React/RCTConvert.h>

#define ReloadBundle   @"ReloadBundle"
#define HotUpdatePath  @"HotUpdateBundle"
#define HotUpdateProgress @"syncProgress"
#define DevBundlePath @"DevRNBundle"

static NSURL *currentURL = nil;

@interface RNHeyteaCodePush() <RCTBridgeModule>

@end

@implementation RNHeyteaCodePush


RCT_EXPORT_MODULE(ReactNativeHeyteaCodepush)


-(dispatch_queue_t)methodQueue{
  return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[HotUpdateProgress];
}

-(void)sendNotificationToJsWithProgress:(NSString *)progress{
  [self sendEventWithName:HotUpdateProgress body: progress];
}

RCT_EXPORT_METHOD(syncHot
                  :(BOOL)restartAfterUpdate
                  :(NSString *)md5
                  :(int)versionCode
                  :(NSString *)url
                  :(RCTResponseSenderBlock)callback
                  ){
  
  NSString *versionStr = [NSString stringWithFormat:@"%d",versionCode];
  NSDictionary *data = @{
    @"md5":md5,
    @"url":url,
    @"versionCode":versionStr,
  };
  
  // 下载
  RNHeyteaDownloader *downloader = [RNHeyteaDownloader instance];
  [downloader downloadWithData:data withResult:^(NSString * _Nonnull code) {
    
    if([code isEqualToString:@"fail"]){
      // 下载失败
      currentURL = nil;
      callback(@[[NSNull null],@"1"]);
        
    }else{
     // 下载成功
      NSString *hotBundle = [[[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0] stringByAppendingPathComponent:HotUpdatePath] stringByAppendingPathComponent:@"bundles"];
      NSString *bundleStr = [hotBundle stringByAppendingFormat:@"/%@/bundle-ios/index/main.bundle",versionStr];
      currentURL = [NSURL URLWithString:bundleStr];
        
      if (restartAfterUpdate) {
        [self postReloadNotification];
      }
      callback(@[@"1",[NSNull null]]);
      
    }
  } withProgress:^(float progress) {
    // 下载进度
    NSString *progressStr = [NSString stringWithFormat:@"%f",progress];
    [self sendNotificationToJsWithProgress:progressStr];
    
  }];
  
}


// js端加载成功的回调
// 加载成功才保存进plist文件中 
RCT_EXPORT_METHOD(loadSuccess){
  NSString *plistPath = [self getBundlePlistPath];
  NSMutableArray *arr = [NSMutableArray arrayWithContentsOfFile:plistPath];
  NSDictionary *dic = [arr lastObject];
  [arr removeLastObject];
  [dic setValue:@"1" forKey:@"status"];
  [arr addObject:dic];
  [arr writeToFile:plistPath atomically:YES];
}

 

/**
 判断是否需要热更新
 */
RCT_EXPORT_METHOD(checkForHotUpdate:(int)versionCode
                  :(RCTPromiseResolveBlock) resolve
                  :(RCTPromiseRejectBlock)reject){
  
  NSFileManager *fm = [NSFileManager defaultManager];
  NSString *bundlePlistPath = [self getBundlePlistPath];
  
  if ([fm fileExistsAtPath:bundlePlistPath]) {
    // 热更新过 判断当前的热更新版本
    NSMutableArray *bundleArr = [NSMutableArray arrayWithContentsOfFile:bundlePlistPath];
    if(bundleArr.count > 0){
      NSString *currentVersion = [bundleArr lastObject][@"version"];
        if ([currentVersion intValue] == versionCode) {
          // 版本号相同 无需热更新
          NSError *err = [NSError errorWithDomain:@"" code:0 userInfo:nil];
          reject(@"0",@"version is same",err);
        }else{
          resolve(@1);
        }
    }else{
      resolve(@1);
    }
  }else{
    // 没有热更新过
    resolve(@1);
  }
  
}

/**
 App版本更新
 本地build号< 传过来的 app更新
 */
RCT_EXPORT_METHOD(checkForAppUpdate:(int)versionCode
                  :(RCTPromiseResolveBlock)resolve
                  :(RCTPromiseRejectBlock)reject ){

  int buidCode = [[[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"] intValue];
  if (versionCode > buidCode) {
    // app 版本更新
    resolve(@1);
  }else {
    // 热更新
    resolve(@0);
  }
}

// 跳转appStore
RCT_EXPORT_METHOD(synciOSApp:(NSString *)url){
  NSURL *appUrl = [NSURL URLWithString:url];
  dispatch_async(dispatch_get_main_queue(), ^{
    if ([[UIApplication sharedApplication] canOpenURL:appUrl]) {
       [[UIApplication sharedApplication] openURL:appUrl];
     }
  });
 
}

// 发送刷新jsbundle通知
-(void)postReloadNotification{
  [[NSNotificationCenter defaultCenter]postNotificationName:ReloadBundle object:nil];
}



// 获取存储bundle数组的plist文件路径
-(NSString *)getBundlePlistPath{
  NSString *docPath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
  NSString *bundlePath = [docPath stringByAppendingPathComponent:@"bundle.plist"];
  return bundlePath;
}

// 获取当前加载热更新包的路径
- (NSString *) getHotUpdatePath{
  NSString *docPath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
  NSString *bundlePath = [docPath stringByAppendingPathComponent:HotUpdatePath];
  return bundlePath;
}

+(NSURL *)bundleURL{
    
    // 首次下载完成 无法校验 只有加载成功才能知道bundle可用 status会置为1
    if (currentURL) {
        return currentURL;
    }
    
    BOOL isDir = NO;
    NSURL *finalUrl;
    NSFileManager *fm = [NSFileManager defaultManager];
    
    // 存储热更版本和路径的plist
    NSString *plistPath = [[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0] stringByAppendingPathComponent:@"bundle.plist"];
    
    // 热更新包存储路径
    NSString *hotBundle = [[[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0] stringByAppendingPathComponent:HotUpdatePath] stringByAppendingPathComponent:@"bundles"];
    
    if ([fm fileExistsAtPath:plistPath isDirectory:&isDir]) {
      NSMutableArray *arr = [NSMutableArray arrayWithContentsOfFile:plistPath];
      if (arr.count > 0) {
        // 有热更包
        // 过滤status == 0的
        NSMutableArray *tempArr = [NSMutableArray array];
        for (NSDictionary *dic in arr) {
            if ([dic[@"status"] isEqualToString:@"1"]) {
                [tempArr addObject:dic];
            }
        }
        if (tempArr.count > 0) {
            NSDictionary *currentDic = [tempArr lastObject];
            NSString *path = currentDic[@"path"];
            NSString *finalStr = [hotBundle stringByAppendingFormat:@"/%@/bundle-ios/index/main.bundle",path];
            finalUrl = [NSURL URLWithString:finalStr];
        }else{
            #ifdef DEBUG
            //bundle文件加载
            NSString *filePath = [NSHomeDirectory() stringByAppendingString:@"/Documents"];
            NSString *devBundleDir = [filePath stringByAppendingPathComponent:DevBundlePath];
            finalUrl = [NSURL URLWithString:[devBundleDir stringByAppendingPathComponent:@"bundles/bundle-ios/index/main.bundle"]];
            #else
            finalUrl = [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
            #endif
        }
       
      }else{
        // 存在plist文件但无内容 这种情况一般不存在
        #ifdef DEBUG
          //bundle文件加载
          NSString *filePath = [NSHomeDirectory() stringByAppendingString:@"/Documents"];
          NSString *devBundleDir = [filePath stringByAppendingPathComponent:DevBundlePath];
          finalUrl = [NSURL URLWithString:[devBundleDir stringByAppendingPathComponent:@"bundles/bundle-ios/index/main.bundle"]];
        #else
          finalUrl = [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
        #endif

      }
    }else{
        #ifdef DEBUG
        //bundle文件加载
        NSString *filePath = [NSHomeDirectory() stringByAppendingString:@"/Documents"];
        NSString *devBundleDir = [filePath stringByAppendingPathComponent:DevBundlePath];
        finalUrl = [NSURL URLWithString:[devBundleDir stringByAppendingPathComponent:@"bundles/bundle-ios/index/main.bundle"]];
        #else
        finalUrl = [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
        #endif
    }
    
    return finalUrl;
}


// 获取当前的viewcontroller
-(UIViewController *) getCurrentViewController{
  UIViewController *currentViewController = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
  BOOL runLoopFind = YES;
     while (runLoopFind) {
         if (currentViewController.presentedViewController) {
             currentViewController = currentViewController.presentedViewController;
         } else {
             if ([currentViewController isKindOfClass:[UINavigationController class]]) {
                 currentViewController = ((UINavigationController *)currentViewController).visibleViewController;
             } else if ([currentViewController isKindOfClass:[UITabBarController class]]) {
                 currentViewController = ((UITabBarController* )currentViewController).selectedViewController;
             } else {
                 break;
             }
         }
     }
     return currentViewController;
}

@end
