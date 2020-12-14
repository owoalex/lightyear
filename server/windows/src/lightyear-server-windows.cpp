//============================================================================
// Name        : lightyear-server-windows.cpp
// Author      : Alex Baldwin
// Version     : 0.0
// License     : GPL Version 3
// Description :
//============================================================================

#include <iostream>
#include <winsock2.h>
#include <windows.h>
#include <shlobj.h>
#include <shellapi.h>
#include <dxgi1_2.h>
#include <d3d11.h>
#include <memory>
#include <algorithm>
#include <string>

template <typename T>
class CComPtrCustom {
	public:

	CComPtrCustom(T *aPtrElement):element(aPtrElement) {}
	CComPtrCustom():element(nullptr) {}

	virtual ~CComPtrCustom() {
		Release();
	}

	T* Detach() {
		auto lOutPtr = element;

		element = nullptr;

		return lOutPtr;
	}

	T* detach() {
		return Detach();
	}

	void Release() {
		if (element == nullptr) {
			return;
		}
		element->Release();
		element = nullptr;
	}

	CComPtrCustom& operator = (T *pElement) {
		Release();

		if (pElement == nullptr)
			return *this;

		auto k = pElement->AddRef();

		element = pElement;

		return *this;
	}

	void Swap(CComPtrCustom& other) {
		T* pTemp = element;
		element = other.element;
		other.element = pTemp;
	}

	T* operator->() {
		return element;
	}

	operator T*() {
		return element;
	}

	operator T*() const {
		return element;
	}


	T* get() {
		return element;
	}

	T* get() const {
		return element;
	}

	T** operator &() {
		return &element;
	}

	bool operator !()const {
		return element == nullptr;
	}

	operator bool()const {
		return element != nullptr;
	}

	bool operator == (const T *pElement)const {
		return element == pElement;
	}


	CComPtrCustom(const CComPtrCustom& aCComPtrCustom) {
		if (aCComPtrCustom.operator!()) {
			element = nullptr;
			return;
		}
		element = aCComPtrCustom;
		auto h = element->AddRef();
		h++;
	}

	CComPtrCustom& operator = (const CComPtrCustom& aCComPtrCustom) {
		Release();
		element = aCComPtrCustom;
		auto k = element->AddRef();
		return *this;
	}

	_Check_return_ HRESULT CopyTo(T** ppT) throw() {
		if (ppT == NULL) {
			return E_POINTER;
		}
		*ppT = element;
		if (element) {
			element->AddRef();
		}
		return S_OK;
	}

	HRESULT CoCreateInstance(const CLSID aCLSID) {
		T* lPtrTemp;
		auto lresult = ::CoCreateInstance(aCLSID, NULL, CLSCTX_INPROC, IID_PPV_ARGS(&lPtrTemp));
		if (SUCCEEDED(lresult)) {
			if (lPtrTemp != nullptr) {
				Release();
				element = lPtrTemp;
			}
		}
		return lresult;
	}

	protected:
	T* element;
};


// Driver types supported
D3D_DRIVER_TYPE gDriverTypes[] =
{
	D3D_DRIVER_TYPE_HARDWARE
};
UINT gNumDriverTypes = ARRAYSIZE(gDriverTypes);

// Feature levels supported
D3D_FEATURE_LEVEL gFeatureLevels[] =
{
	D3D_FEATURE_LEVEL_11_0,
	D3D_FEATURE_LEVEL_10_1,
	D3D_FEATURE_LEVEL_10_0,
	D3D_FEATURE_LEVEL_9_1
};

UINT gNumFeatureLevels = ARRAYSIZE(gFeatureLevels);


struct VisualFrame {
	uint32_t width;
	uint32_t height;
	uint8_t* pData;
};

VisualFrame* newFrame(int width, int height) {
	uint8_t* pData = (uint8_t*) malloc(width * height * 3);
	struct VisualFrame* pFrame = (struct VisualFrame*) malloc(sizeof(VisualFrame));
	pFrame->width = width;
	pFrame->height = height;
	pFrame->pData = pData;
	return pFrame;
}

//https://www.codeproject.com/Tips/1116253/Desktop-screen-capture-on-Windows-via-Windows-Desk
void captureFrame(VisualFrame* pFrame) {
	std::cout << "Capture Frame" << std::endl;

	CComPtrCustom<ID3D11Device> lDevice;
	CComPtrCustom<ID3D11DeviceContext> lImmediateContext;
	CComPtrCustom<IDXGIOutputDuplication> lDeskDupl;
	CComPtrCustom<ID3D11Texture2D> lAcquiredDesktopImage;
	CComPtrCustom<ID3D11Texture2D> lGDIImage;
	CComPtrCustom<ID3D11Texture2D> lDestImage;
	DXGI_OUTPUT_DESC lOutputDesc;
	DXGI_OUTDUPL_DESC lOutputDuplDesc;

	do {
		D3D_FEATURE_LEVEL lFeatureLevel;

		HRESULT hr(E_FAIL);

		// Create device
		for (UINT DriverTypeIndex = 0; DriverTypeIndex < gNumDriverTypes; ++DriverTypeIndex) {
			hr = D3D11CreateDevice(
				nullptr,
				gDriverTypes[DriverTypeIndex],
				nullptr,
				0,
				gFeatureLevels,
				gNumFeatureLevels,
				D3D11_SDK_VERSION,
				&lDevice,
				&lFeatureLevel,
				&lImmediateContext);

			if (SUCCEEDED(hr)) {
				// Device creation success, no need to loop anymore
				break;
			}

			lDevice.Release();
			lImmediateContext.Release();
		}

		if (FAILED(hr))
			break;

		//Sleep(100);

		if (lDevice == nullptr)
			break;

		// Get DXGI device
		CComPtrCustom<IDXGIDevice> lDxgiDevice;

		hr = lDevice->QueryInterface(IID_PPV_ARGS(&lDxgiDevice));

		if (FAILED(hr))
			break;

		// Get DXGI adapter
		CComPtrCustom<IDXGIAdapter> lDxgiAdapter;
		hr = lDxgiDevice->GetParent(
			__uuidof(IDXGIAdapter),
			reinterpret_cast<void**>(&lDxgiAdapter));

		if (FAILED(hr))
			break;

		lDxgiDevice.Release();

		UINT Output = 0;

		// Get output
		CComPtrCustom<IDXGIOutput> lDxgiOutput;
		hr = lDxgiAdapter->EnumOutputs(
			Output,
			&lDxgiOutput);

		if (FAILED(hr))
			break;

		lDxgiAdapter.Release();

		hr = lDxgiOutput->GetDesc(
			&lOutputDesc);

		if (FAILED(hr))
			break;

		// QI for Output 1
		CComPtrCustom<IDXGIOutput1> lDxgiOutput1;

		hr = lDxgiOutput->QueryInterface(IID_PPV_ARGS(&lDxgiOutput1));

		if (FAILED(hr))
			break;

		lDxgiOutput.Release();

		// Create desktop duplication
		hr = lDxgiOutput1->DuplicateOutput(
			lDevice,
			&lDeskDupl);

		if (FAILED(hr))
			break;

		lDxgiOutput1.Release();

		// Create GUI drawing texture
		lDeskDupl->GetDesc(&lOutputDuplDesc);

		D3D11_TEXTURE2D_DESC desc;

		desc.Width = lOutputDuplDesc.ModeDesc.Width;

		desc.Height = lOutputDuplDesc.ModeDesc.Height;

		desc.Format = lOutputDuplDesc.ModeDesc.Format;

		desc.ArraySize = 1;

		desc.BindFlags = D3D11_BIND_FLAG::D3D11_BIND_RENDER_TARGET;

		desc.MiscFlags = D3D11_RESOURCE_MISC_GDI_COMPATIBLE;

		desc.SampleDesc.Count = 1;

		desc.SampleDesc.Quality = 0;

		desc.MipLevels = 1;

		desc.CPUAccessFlags = 0;

		desc.Usage = D3D11_USAGE_DEFAULT;

		hr = lDevice->CreateTexture2D(&desc, NULL, &lGDIImage);

		if (FAILED(hr))
			break;

		if (lGDIImage == nullptr)
			break;


		// Create CPU access texture

		desc.Width = lOutputDuplDesc.ModeDesc.Width;

		desc.Height = lOutputDuplDesc.ModeDesc.Height;

		desc.Format = lOutputDuplDesc.ModeDesc.Format;

		desc.ArraySize = 1;

		desc.BindFlags = 0;

		desc.MiscFlags = 0;

		desc.SampleDesc.Count = 1;

		desc.SampleDesc.Quality = 0;

		desc.MipLevels = 1;

		desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ | D3D11_CPU_ACCESS_WRITE;
		desc.Usage = D3D11_USAGE_STAGING;

		hr = lDevice->CreateTexture2D(&desc, NULL, &lDestImage);

		if (FAILED(hr))
			break;

		if (lDestImage == nullptr)
			break;

		CComPtrCustom<IDXGIResource> lDesktopResource;
		DXGI_OUTDUPL_FRAME_INFO lFrameInfo;

		int lTryCount = 4;

		do
		{

			Sleep(100);

			// Get new frame
			hr = lDeskDupl->AcquireNextFrame(
				250,
				&lFrameInfo,
				&lDesktopResource);

			if (SUCCEEDED(hr))
				break;

			if (hr == DXGI_ERROR_WAIT_TIMEOUT)
			{
				continue;
			}
			else if (FAILED(hr))
				break;

		} while (--lTryCount > 0);

		if (FAILED(hr))
			break;

		// QI for ID3D11Texture2D

		hr = lDesktopResource->QueryInterface(IID_PPV_ARGS(&lAcquiredDesktopImage));

		if (FAILED(hr))
			break;

		lDesktopResource.Release();

		if (lAcquiredDesktopImage == nullptr)
			break;

		// Copy image into GDI drawing texture

		lImmediateContext->CopyResource(lGDIImage, lAcquiredDesktopImage);


		// Draw cursor image into GDI drawing texture

		CComPtrCustom<IDXGISurface1> lIDXGISurface1;

		hr = lGDIImage->QueryInterface(IID_PPV_ARGS(&lIDXGISurface1));

		if (FAILED(hr))
			break;

		CURSORINFO lCursorInfo = { 0 };

		lCursorInfo.cbSize = sizeof(lCursorInfo);

		auto lBoolres = GetCursorInfo(&lCursorInfo);

		if (lBoolres == TRUE)
		{
			if (lCursorInfo.flags == CURSOR_SHOWING)
			{
				auto lCursorPosition = lCursorInfo.ptScreenPos;

				auto lCursorSize = lCursorInfo.cbSize;

				HDC  lHDC;

				lIDXGISurface1->GetDC(FALSE, &lHDC);

				DrawIconEx(
					lHDC,
					lCursorPosition.x,
					lCursorPosition.y,
					lCursorInfo.hCursor,
					0,
					0,
					0,
					0,
					DI_NORMAL | DI_DEFAULTSIZE);

				lIDXGISurface1->ReleaseDC(nullptr);
			}
		}

		// TEST

		//lGDIImage

		//printf("Pointer to info: %16x", 32523);

		// Copy image into CPU access texture

		lImmediateContext->CopyResource(lDestImage, lGDIImage);

		// Copy from CPU access texture to bitmap buffer

		D3D11_MAPPED_SUBRESOURCE resource;
		UINT subresource = D3D11CalcSubresource(0, 0, 0);
		lImmediateContext->Map(lDestImage, subresource, D3D11_MAP_READ_WRITE, 0, &resource);

		// Try to extract bytes

		D3D11_TEXTURE2D_DESC descOutput;
		lGDIImage->GetDesc(&descOutput);

		printf("Pointer to (Destination) info: 0x%08x\n", &descOutput);
		printf("Image width: %d\n", desc.Width);
		printf("Image height: %d\n", desc.Height);

		//printf("Pointer to (Destination) data: 0x%08x\n", resource.pData);



		pFrame->width = desc.Width;
		pFrame->height = desc.Height;
		pFrame->pData = (uint8_t*) malloc(pFrame->width * pFrame->height * 3);
		//uint32_t width;
		//	uint32_t height;
		//	uint8_t* pData;

		byte* colorGroupPointer = nullptr;
		//byte r;
		//byte g;
		//byte b;
		for (int y = 0; y < pFrame->height; y++) {
			for (int x = 0; x < pFrame->width; x++) {
				colorGroupPointer = (uint8_t*) (resource.pData + ((x + (y * pFrame->width)) * 4));
				pFrame->pData[((x + (y * pFrame->width)) * 3) + 0] = *(colorGroupPointer + 0);
				pFrame->pData[((x + (y * pFrame->width)) * 3) + 1] = *(colorGroupPointer + 1);
				pFrame->pData[((x + (y * pFrame->width)) * 3) + 2] = *(colorGroupPointer + 2);
			}
		}

	} while (false);

	std::cout << "Done Capture Frame" << std::endl;

}

int main() {
	int port = 75535;
	char* host = "192.168.1.8";
	std::cout << "Connecting to: " << host << ":" << port << std::endl; // prints !!!Hello World!!!

	VisualFrame* pFrame = newFrame(1920, 1080);
	captureFrame(pFrame);

	for (int y = 0; y < 64; y++) {
		for (int x = 0; x < 128; x++) {
			uint8_t* colorGroupPointer = (uint8_t*) (pFrame->pData + ((x + (y * pFrame->width)) * 3));
			uint8_t r = *(colorGroupPointer);
			uint8_t g = *(colorGroupPointer + 1);
			uint8_t b = *(colorGroupPointer + 2);
			if (r+g+b > (200 * 3)) {
				printf("##");
			} else {
				printf("  ");
			}
		}
		printf("\n");
	}

	return 0;
}
