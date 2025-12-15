reaimport { TestBed } from '@angular/core/testing';
import { App } from './app';

/**
 * AUDIT TEST: Frontend Application Tests
 * These tests verify the Angular application works correctly
 * Tests run with Karma/Jasmine during the Jenkins pipeline
 */
describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Hello, Frontend');
  });

  it('should have a valid component instance', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeDefined();
    expect(fixture.componentInstance.constructor.name).toBe('App');
  });

  it('should render without errors', () => {
    const fixture = TestBed.createComponent(App);
    expect(() => fixture.detectChanges()).not.toThrow();
  });

  it('should have proper DOM structure', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')).toBeTruthy();
  });

  /**
   * INTENTIONALLY BREAKABLE TEST FOR AUDIT DEMO
   * Uncomment this test to demonstrate Jenkins pipeline failure on test failure
   * This will cause the frontend tests to fail and the pipeline to halt
   */
  // it('should fail intentionally for audit demo', () => {
  //   fail('This test is intentionally failing to demonstrate Jenkins test failure detection');
  // });
});
