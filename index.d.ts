/**
 * 加载bundle成功后，调用该函数，用于判断热更新的成功、失败，以判断是否回滚操作
 */
export function loadSuccess(): void;

/**
 * 检查是否需要热更新
 * @param versionCode 热更新文件的versionCode
 */
export function checkForHotUpdate(versionCode: number): Promise<boolean>;

/**
 * 检查是否需要App更新
 * @param versionCode App的versionCode, iOS对应versionNumber
 */
export function checkForAppUpdate(versionCode: number): Promise<boolean>;

/**
 * 同步热更新，与微软CodePush基本一致
 */
export function syncHot(
    restartAfterUpdate: boolean,
    md5: string,
    versionCode: number,
    url: string,
    progressCallback: (progress: number) => void,
    successCallback: () => void,
    errorCallback: (e: Error) => void
): void;

/**
 * 同步安卓App更新，下载apk并提示安装
 */
export function syncAndroidApp(
    md5: string,
    versionCode: number,
    url: string,
    progressCallback: (progress: number) => void,
    successCallback: () => void,
    errorCallback: (e: Error) => void
): void;

/**
 * 同步iOS App更新，跳转App store
 * @param {string} url App Store应用链接
 */
export function synciOSApp(url: string): void;