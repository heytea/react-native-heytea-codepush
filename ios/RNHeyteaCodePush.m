//
//  RCTUdesk.m
//  heyteago
//
//  Created by Chris Zhou on 2020/6/11.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "RNHeyteaCodePush.h"
#import <MBProgressHUD.h>
#import <SSZipArchive.h>

#define ReloadBundle   @"ReloadBundle"
#define HotUpdatePath  @"HotUpdateBundle"


@protocol RNCodePushDelegate <NSObject>
-(void)reloadBundle;
@end

@interface RNHeyteaCodePush() <RCTBridgeModule,NSURLSessionDownloadDelegate>

@property(nonatomic,strong)MBProgressHUD *hud;
@property(nonatomic,strong)RCTResponseSenderBlock progressCallback;
@property(nonatomic,strong)RCTResponseSenderBlock successCallback;
@property(nonatomic,strong)RCTResponseSenderBlock failCallback;
@property(nonatomic,copy)NSString *versionCode;
@property(nonatomic,copy)NSString *downloadUrl;

@property(nonatomic,weak) id <RNCodePushDelegate> codepushDelegate;

@end

@implementation RNHeyteaCodePush

-(instancetype)init{
  if (self = [super init]) {
    
  }
  return self;
}

RCT_EXPORT_MODULE(ReactNativeHeyteaCodepush)


//-(dispatch_queue_t)methodQueue{
//  return dispatch_get_main_queue();
//}


RCT_EXPORT_METHOD( syncHot:(NSDictionary *)data
                  progress:(RCTResponseSenderBlock)progressCallback
                  success:(RCTResponseSenderBlock)successCallback
                  fail:(RCTResponseSenderBlock)failCallback
                  ){
  self.downloadUrl = data[@"url"];
  self.progressCallback = progressCallback;
  self.successCallback = successCallback;
  self.failCallback = failCallback;
  
  // 下载bundle
//  UIViewController *currentVc = [self getCurrentViewController];
//  MBProgressHUD *hud = [MBProgressHUD showHUDAddedTo:currentVc.view animated:YES];
//  hud.mode = MBProgressHUDModeAnnularDeterminate;
//  hud.label.text = @"下载中...";
//  self.hud = hud;
  [self downloadBundle];
  
  
}

RCT_EXPORT_METHOD(loadSuccess){
  
}

/**
 判断是否需要热更新
 */
RCT_EXPORT_METHOD( checkForHotUpdate:(NSString *) versionCode
                  resolve:(RCTPromiseResolveBlock) resolve
                  reject:(RCTPromiseRejectBlock)reject ){
  
  self.versionCode = versionCode;
  NSFileManager *fm = [NSFileManager defaultManager];
  NSString *bundlePlistPath = [self getBundlePlistPath];
  
  if ([fm fileExistsAtPath:bundlePlistPath]) {
    // 热更新过 判断当前的热更新版本
    NSMutableArray *bundleArr = [NSMutableArray arrayWithContentsOfFile:bundlePlistPath];
    if(bundleArr.count > 0){
      NSString *currentVersion = [bundleArr lastObject][@"version"];
        if (currentVersion == versionCode) {
          // 版本号相同 无需热更新
          NSError *err = [NSError errorWithDomain:@"" code:0 userInfo:nil];
          reject(@"0",@"",err);
        }else{
          resolve(@"");
        }
    }else{
      NSError *err = [NSError errorWithDomain:@"" code:0 userInfo:nil];
      reject(@"0",@"",err);
    }
  }else{
    // 没有热更新过
    resolve(@"");
  }
  
}

/**
 App版本更新
 */
RCT_EXPORT_METHOD( checkForAppUpdate:(int) versionCode
                  resolve:(RCTPromiseResolveBlock) resolve
                  reject:(RCTPromiseRejectBlock)reject ){
  NSURL *appUrl = [NSURL URLWithString:@"itms-apps://itunes.apple.com/app/id1142110895"];
  if ([[UIApplication sharedApplication] canOpenURL:appUrl]) {
    [[UIApplication sharedApplication] openURL:appUrl];
  }
}


/**
 下载更新包
 */
-(void)downloadBundle{
   // 下载
    NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration] delegate:self delegateQueue:[NSOperationQueue mainQueue]];
    NSURL *url = [NSURL URLWithString:self.downloadUrl];
    NSURLRequest *request = [NSURLRequest requestWithURL:url];
    NSURLSessionDownloadTask *task = [session downloadTaskWithRequest:request];
    [task resume];
 }


// 下载成功
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask
didFinishDownloadingToURL:(NSURL *)location{
//  if(self.hud){
//    [self.hud hideAnimated:YES];
//  }
  self.successCallback(@[]);
  
  // 保存在 HotUpdateBundle/bundles 下
  // path 和code。 保存到字典 存入数组 写入 bundle.plist
  BOOL isDir = NO;
  NSFileManager *fm = [NSFileManager defaultManager];

  NSString *hotPath = [self getHotUpdatePath];
  NSString *hotBundlesPath = [hotPath stringByAppendingPathComponent:@"bundles"];
  NSString *plistPath = [self getBundlePlistPath];
  
   if (![fm fileExistsAtPath:hotBundlesPath isDirectory:nil]) {
     [fm createDirectoryAtPath:hotBundlesPath withIntermediateDirectories:YES attributes:nil error:nil];
   }
  
  // 解压到bundles 文件夹下
   NSString *curBundlePath = [hotBundlesPath stringByAppendingPathComponent:self.versionCode];
   [SSZipArchive unzipFileAtPath:location.path toDestination:curBundlePath];
  
  // 把版本号和bundle路径保存到plist文件
   NSMutableArray *plistArr = [NSMutableArray array];
   if ([fm fileExistsAtPath:plistPath isDirectory:&isDir]) {
     plistArr = [NSMutableArray arrayWithContentsOfFile:plistPath];
     NSDictionary *dic = @{@"version":self.versionCode,@"path":self.versionCode};
     [plistArr addObject:dic];
     [plistArr writeToFile:plistPath atomically:YES];
   }else{
     NSDictionary *dic = @{@"version":self.versionCode,@"path":self.versionCode};
     [plistArr addObject:dic];
     [plistArr writeToFile:plistPath atomically:YES];
   }
  
  // 刷新jsbundle
//  if (self.codepushDelegate && [self.codepushDelegate respondsToSelector:@selector(reloadBundle)]) {
//    [self.codepushDelegate reloadBundle];
//  }
  [[NSNotificationCenter defaultCenter] postNotificationName:ReloadBundle object:nil userInfo:nil];

}

// 下载失败
- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task
didCompleteWithError:(nullable NSError *)error{
//  if (self.hud) {
//    [self.hud hideAnimated:YES];
//  }
  self.failCallback(@[]);
}

// 下载进度
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask
             didWriteData:(int64_t)bytesWritten
        totalBytesWritten:(int64_t)totalBytesWritten
totalBytesExpectedToWrite:(int64_t)totalBytesExpectedToWrite{
  float progress = (float) totalBytesWritten / totalBytesExpectedToWrite;
//  if (self.hud) {
//    self.hud.progress = progress;
//  }
  self.progressCallback(@[[NSNumber numberWithFloat:progress]]);
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
