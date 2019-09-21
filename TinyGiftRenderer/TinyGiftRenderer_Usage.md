# TinyGiftRenderer接口文档


## **auth函数**
鉴权函数，必须在调用sdk其他接口前执行，获取sdk授权
```Java
int auth(Context context, String authKey, int authKeyLength)
```
参数
- authKey：授权码
- authKeyLength：授权码字符串长度

返回值
- 返回0

## **setAuthEventListener函数**
设置鉴权监听函数，如果鉴权失败，将会通过监听函数通知鉴权结果
```Java
void setAuthEventListener(IEventListener listener)
```
参数
- listener：监听函数

返回值: 无

## **TinyGiftRenderer构造函数**
创建渲染对象
```Java
public TinyGiftRenderer(Context context, int vertFlip)
```
参数
- vertFlip：是否上下翻转画面，0：不开启，非0：开启

返回值：无

## **setGiftPath函数**
设置要渲染的特效资源配置json文件的路径，路径指向json文件名称
```Java
void setGiftPath(String effect)
```
参数：
- effect：特效的json配置文件路径，可以是sdcard上的绝对路径，也可以是Android Assets路径（以"assets/"开头）

返回值： 无

## **renderGift函数**
渲染特效，以bgTexId作为背景。
这个函数必须在OpenGL ES 3.0 context中执行，因此必须提前创建好opengl环境。
```Java
int renderGift(int bgTexId, int width, int height)
```
参数：
- bgTexId：OpenGL Texture对象，用作背景渲染。可以将相机采集的图像加载进入opengl texture中，将texture作为背景纹理bgTeXId。非0值表示一个有效的纹理，0则不渲染背景
- width：纹理图像宽度
- height：纹理图像高度

返回值：
- 0：成功，非0：失败

## **rotate函数**
设置渲染模型围绕x，y，z坐标轴的旋转角度
```Java
int rotate(float x, float y, float z)
```
参数：
- x: 模型应该绕x轴渲染的角度
- y: 模型应该绕y轴渲染的角度
- z: 模型应该绕z轴渲染的角度

返回值：
- 返回0

## **translate函数**
设置模型在x，y，z坐标轴上的平移距离
```Java
int translate(float x, float y, float z)
```
参数：
- x: 模型在x轴上的平移
- y: 模型在y轴上的平移
- z: 模型在z轴上的平移

返回值：
- 返回0

## **scale函数**
设置模型在x，y，z坐标轴上的缩放因子，1表示不缩放，>1: 模型放大，<1: 模型缩小
```Java
int scale(float x, float y, float z)
```
参数：
- x：模型在x轴上的缩放
- y：模型在y轴上的缩放
- z：模型在z轴上的缩放

返回值：
- 返回0

## **pauseRendering函数**
暂停渲染
```Java
int pauseRendering()
```
返回值：  
- 返回0

## **resumeRendering函数**
从暂停状态恢复渲染
```Java
int resumeRendering()
```
返回值：
- 返回0

## **setRenderParam函数**
设置渲染参数
```Java
int setRenderParams(String paramName, int value)
```
参数：
- paramName：参数名称
- value: 参数的值

返回值：
- 返回0

*备注：当前支持的参数有：*
- paramNam: "AssetManager"： 表示设置的value是AssetManager对象


# 资源制作方法
在3DMax中加载obj模型，将挂件png图片移动到所需的位置，然后将图片和obj一起导出，整个制作流程与FaceUnity的FUEditor的制作流程类似：https://github.com/Faceunity/FUEditor